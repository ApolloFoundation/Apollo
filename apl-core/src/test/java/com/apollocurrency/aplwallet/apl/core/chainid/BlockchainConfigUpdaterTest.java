/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.config.ShardingSettings;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
class BlockchainConfigUpdaterTest {

    // config is used for simulating complex shard settings
    private static final String CONFIG_NAME = "long-list-for-sharding-config.json";
    private BlockchainConfigUpdater configUpdater;
    private PropertiesHolder holder = mock(PropertiesHolder.class);
    private ChainsConfigLoader chainsConfigLoader;
    private Map<UUID, Chain> loadedChains;
    private BlockchainConfig blockchainConfig;
    private Chain chain;

    @BeforeEach
    void setup() {
        chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        // module registration is needed for reading sub-components like
        //  blockchainProperties -> 'shardSettings' / 'consensusSettings' in UNIT test
        ChainsConfigLoader.getMAPPER().registerModule(new ParanamerModule());
        loadedChains = chainsConfigLoader.load();
        assertEquals(1, loadedChains.size());
        assertNotNull(loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6")));
        doReturn(5000).when(holder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(holder).getIntProperty("apl.minPrunableLifetime");
    }

    @Test
    void testUninitializedUpdater() {
        // NOT CALLED prepareAndInitComponents();
        // so it's not properly initialized component
        configUpdater = new BlockchainConfigUpdater(null, null);
        ChainsConfigLoader.getMAPPER().registerModule(new ParanamerModule());
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(0);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetMissingShardSettingsByZeroHeight() {
        prepareAndInitComponents();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(0);
        log.trace("result = {}", result);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertFalse(result.get().isEnabled());
        assertNotNull(configUpdater.getShardingSettingsMap());
    }

    @Test
    void testGetDisabledConfigByOne() {
        prepareAndInitComponents();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(1);
        log.trace("result = {}", result);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertFalse(result.get().isEnabled());
        assertNotNull(configUpdater.getShardingSettingsMap());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -100, -1 })
    void test_Negative_Height_Configs(int trimHeight) {
        prepareAndInitComponents();
        assertThrows(RuntimeException.class, () -> {
            configUpdater.getShardingSettingsByTrimHeight(trimHeight);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 10, 20 })
    void test_Positive_Disabled_Configs(int trimHeight) {
        prepareAndInitComponents();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(trimHeight);
        log.trace("result = {}", result);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertFalse(result.get().isEnabled());
        assertNotNull(configUpdater.getShardingSettingsMap());
    }

    @Test
    void testGetEmptyByTwo() {
        prepareAndInitComponents();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(2);
        log.trace("result = {}", result);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertTrue(result.get().isEnabled());
        assertEquals(2, result.get().getFrequency());
        assertNotNull(configUpdater.getShardingSettingsMap());
    }

    @ParameterizedTest
    @MethodSource("provideTrimHeightAndFrequency")
    void testAllCorrectConfigsWithFrequency(int trimHeight, int shardFrequency) {
        prepareAndInitComponents();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(trimHeight);
        log.trace("result = {}", result);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertTrue(result.get().isEnabled(), "got = " + result.get());
        assertEquals(shardFrequency, result.get().getFrequency(),
            String.format("expected = %d , got = %d", shardFrequency, result.get().getFrequency()));
        assertNotNull(configUpdater.getShardingSettingsMap());
    }

    @ParameterizedTest
    @MethodSource("provideTrimHeightAndFrequency")
    void test_atHeight_AllCorrectConfigsWithFrequency(int trimHeight, int shardFrequency) {
        prepareAndInitComponents();
        HeightConfig  result = blockchainConfig.getConfigAtHeight(trimHeight);  // checked method
        log.trace("result = {}", result);
        assertNotNull(result);
        assertTrue(result.isShardingEnabled(), "got = " + result.isShardingEnabled());
        assertEquals(shardFrequency, result.getShardingFrequency(),
            String.format("expected = %d , got = %d", shardFrequency, result.getShardingFrequency()));
    }

    /**
     * Height and target Frequency are supplied into unit test method
     * @return height + frequency value for test
     */
    static Stream<Arguments> provideTrimHeightAndFrequency() {
        return Stream.of(
            arguments(2, 2), // trim height 2 corresponds to configured frequency = 2
//---            arguments(3, 2), // should be NOT correct
//---            arguments(4, 2), // should be NOT correct
            arguments(5, 5), // trim height 5 corresponds to configured frequency = 5
            arguments(6, 5), // trim height 6 corresponds to configured frequency = 5
            arguments(9, 5),
//---            arguments(10, 5), // should be NOT correct
//---            arguments(13, 5), // should be NOT correct
            arguments(15, 15),
            arguments(16, 15),
            arguments(19, 15),
//---            arguments(20, 15), // should be NOT correct
//---            arguments(29, 15), // should be NOT correct
            arguments(30, 30),
            arguments(31, 30),
            arguments(39, 30),
            arguments(40, 40),
            arguments(50, 40),
            arguments(1025, 40),
            arguments(Integer.MAX_VALUE, 40)
        );
    }

    @ParameterizedTest
    @MethodSource("provideDisabledResponseForTrimHeightAndFrequency")
    void testAll_INCorrect_ConfigsWithFrequency(int trimHeight, int shardFrequency) {
        prepareAndInitComponents();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(trimHeight);
        log.trace("result = {}", result);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertFalse(result.get().isEnabled(), "got = " + result.get());
        assertNotEquals(shardFrequency, result.get().getFrequency(),
            String.format("expected = %d , got = %d", shardFrequency, result.get().getFrequency()));
        assertNotNull(configUpdater.getShardingSettingsMap());
    }

    @ParameterizedTest
    @MethodSource("provideDisabledResponseForTrimHeightAndFrequency")
    void test_AtHeight_INCorrect_ConfigsWithFrequency(int trimHeight, int shardFrequency) {
        prepareAndInitComponents();
        HeightConfig result = blockchainConfig.getConfigAtHeight(trimHeight);  // checked method
        log.trace("result = {}", result);
        assertNotNull(result);
        assertFalse(result.isShardingEnabled(), "got = " + result.isShardingEnabled());
        assertNotEquals(shardFrequency, result.getShardingFrequency(),
            String.format("expected = %d , got = %d", shardFrequency, result.getShardingFrequency()));
    }

    /**
     * Height and target Frequency are supplied into unit test method
     * @return height + frequency value for test
     */
    static Stream<Arguments> provideDisabledResponseForTrimHeightAndFrequency() {
        return Stream.of(
            // ALL should be NOT correct
            arguments(3, 2), // trim height 3 corresponds to configured frequency = 2
            arguments(4, 2), // trim height 4 corresponds to configured frequency = 2
            arguments(10, 5),
            arguments(13, 5),
            arguments(20, 15),
            arguments(21, 15),
            arguments(29, 15)
        );
    }

    private void prepareAndInitComponents() {
        chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
        assertNotNull(chain);
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, holder);
        ChainsConfigLoader.getMAPPER().registerModule(new ParanamerModule());
        configUpdater = new BlockchainConfigUpdater(blockchainConfig, null);
        configUpdater.updateChain(chain, holder);
    }


}