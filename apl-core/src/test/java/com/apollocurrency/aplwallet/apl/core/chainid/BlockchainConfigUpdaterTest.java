/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.chainid;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.config.ShardingSettings;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
class BlockchainConfigUpdaterTest {

    // config is used for simulating complex shard settings
    private static final String CONFIG_NAME = "long-list-for-sharding-config.json";
    private BlockchainConfigUpdater configUpdater;
    private PropertiesHolder holder = mock(PropertiesHolder.class);
    private ChainsConfigLoader chainsConfigLoader;
    private Map<UUID, Chain> loadedChains;

    @BeforeEach
    void setup() {
        chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        loadedChains = chainsConfigLoader.load();
        assertEquals(1, loadedChains.size());
        assertNotNull(loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6")));
        doReturn(5000).when(holder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(holder).getIntProperty("apl.minPrunableLifetime");
    }

    @Test
    void testUninitializedUpdater() {
        configUpdater = new BlockchainConfigUpdater(null, null);
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(0);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetEmptyByZeroHeight() {
        prepareAndInitConfigUpdater();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(0);
        log.trace("result = {}", result);
        assertTrue(result.isEmpty());
        assertNotNull(configUpdater.getEnabledShardingSettingsMap());
    }

    @Test
    void testGetEmptyByOne() {
        prepareAndInitConfigUpdater();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(1);
        log.trace("result = {}", result);
        assertTrue(result.isEmpty());
        assertNotNull(configUpdater.getEnabledShardingSettingsMap());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -100, -1, 0, 1 })
    void testAllZeroConfigs(int trimHeight) {
        prepareAndInitConfigUpdater();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(trimHeight);
        log.trace("result = {}", result);
        assertTrue(result.isEmpty());
        assertNotNull(configUpdater.getEnabledShardingSettingsMap());
    }

    @Test
    void testGetEmptyByTwo() {
        prepareAndInitConfigUpdater();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(2);
        log.trace("result = {}", result);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertTrue(result.get().isEnabled());
        assertEquals(2, result.get().getFrequency());
        assertNotNull(configUpdater.getEnabledShardingSettingsMap());
    }

    @ParameterizedTest
    @MethodSource("provideTrimHeightAndFrequency")
    void testAllCorrectConfigsWithFrequency(int trimHeight, int shardFrequency) {
        prepareAndInitConfigUpdater();
        Optional<ShardingSettings> result = configUpdater.getShardingSettingsByTrimHeight(trimHeight);
        log.trace("result = {}", result);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertTrue(result.get().isEnabled());
        assertEquals(shardFrequency, result.get().getFrequency());
        assertNotNull(configUpdater.getEnabledShardingSettingsMap());
    }

    /**
     * Height and target Frequency are supplied into unit test method
     * @return height + frequency value for test
     */
    static Stream<Arguments> provideTrimHeightAndFrequency() {
        return Stream.of(
            arguments(2, 2), // trim height 2 corresponds to configured frequency = 2
            arguments(3, 2), // trim height 3 corresponds to configured frequency = 2
            arguments(4, 2), // trim height 4 corresponds to configured frequency = 2
            arguments(5, 5), // trim height 5 corresponds to configured frequency = 5
            arguments(6, 5), // trim height 6 corresponds to configured frequency = 5
            arguments(10, 5),
            arguments(13, 5),
            arguments(15, 15),
            arguments(16, 15),
            arguments(19, 15),
            arguments(20, 15), // shard config at height = 20 is DISABLE
            arguments(29, 15), // shard config at height = 20 is DISABLE
            arguments(30, 30),
            arguments(31, 30),
            arguments(39, 30),
            arguments(40, 40),
            arguments(50, 40),
            arguments(1025, 40),
            arguments(Integer.MAX_VALUE, 40)
        );
    }
    private void prepareAndInitConfigUpdater() {
        Chain chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
        assertNotNull(chain);
        assertNotNull(chain.getBlockchainProperties());
        configUpdater = new BlockchainConfigUpdater(new BlockchainConfig(chain, holder), null);
        configUpdater.updateChain(chain, holder);
    }


}