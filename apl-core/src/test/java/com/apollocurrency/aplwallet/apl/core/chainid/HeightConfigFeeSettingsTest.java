/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
class HeightConfigFeeSettingsTest {

    // config is used for simulating complex shard settings
    private static final String CONFIG_NAME = "chains-fee-config.json";
    private PropertiesHolder holder = mock(PropertiesHolder.class);
    private ChainsConfigLoader chainsConfigLoader;
    private Map<UUID, Chain> loadedChains;
    private BlockchainConfig blockchainConfig;
    private Chain chain;

    @BeforeEach
    void setup() {
        chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        loadedChains = chainsConfigLoader.load();
        assertEquals(1, loadedChains.size());
        assertNotNull(loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6")));
        doReturn(5000).when(holder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(holder).getIntProperty("apl.minPrunableLifetime");
        prepareAndInitComponents();
    }

    @ParameterizedTest
    @MethodSource("provideHeightAndFeeRate")
    void test_atHeight_AllCorrectConfigs(int height, int rate) {

        HeightConfig  result = blockchainConfig.getConfigAtHeight(height);  // checked method
        log.trace("result = {}", result);
        assertNotNull(result);
        assertEquals(rate, result.getFeeRate((byte)0,(byte)0));
        assertEquals(rate, result.getFeeRate((byte)0,(byte)1));
        assertEquals(rate, result.getFeeRate((byte)1,(byte)1));
        assertEquals(100, result.getFeeRate((byte)10,(byte)10));//default rate for other type and subType

        //assertEquals(shardFrequency, result.getShardingFrequency());
    }

    /**
     * Height and target Fee Rate are supplied into unit test method
     * @return height + fee rate value for test
     */
    static Stream<Arguments> provideHeightAndFeeRate() {
        return Stream.of(
            arguments(1, 100),
            arguments(2, 100), // trim height 5 corresponds to configured frequency = 5
            arguments(10,100), // trim height 6 corresponds to configured frequency = 5
            arguments(50, 50),
            arguments(55, 50),
            arguments(70, 100),
            arguments(100, 0),
            arguments(110, 0),
            arguments(120, 0),
            arguments(200, 100),
            arguments(210, 100),
            arguments(Integer.MAX_VALUE, 100)
        );
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
    }

}