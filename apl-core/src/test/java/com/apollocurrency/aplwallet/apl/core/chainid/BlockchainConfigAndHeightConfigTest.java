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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
class BlockchainConfigAndHeightConfigTest {

    // config is used for simulating complex shard settings
    private static final String CONFIG_NAME = "long-list-for-sharding-config.json";
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
    @MethodSource("provideTrimHeightAndFrequency")
    void test_atHeight_AllCorrectConfigsWithFrequency(int trimHeight, int shardFrequency) {
        HeightConfig  result = blockchainConfig.getConfigAtHeight(trimHeight);  // checked method
        log.trace("result = {}", result);
        assertNotNull(result);
        assertTrue(result.isShardingEnabled(), "got = " + result.isShardingEnabled());
        assertEquals(shardFrequency, result.getShardingFrequency(),
            String.format("expected = %d , got = %d", shardFrequency, result.getShardingFrequency()));
    }

    @ParameterizedTest
    @MethodSource("provideHeightAndMaxNumberOfChileAccounts")
    void test_atHeight_AllConfigsWithNumberOfChildAccounts(int height, int childNumber) {
        HeightConfig  result = blockchainConfig.getConfigAtHeight(height);  // checked method
        assertNotNull(result);
        assertEquals(childNumber, result.getMaxNumberOfChildAccount());
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
    void test_AtHeight_INCorrect_ConfigsWithFrequency(int trimHeight, int shardFrequency) {
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

    /**
     * Height and number of the child accounts are supplied into unit test method
     * @return height + frequency value for test
     */
    static Stream<Arguments> provideHeightAndMaxNumberOfChileAccounts() {
        // the second arg is the maxNumberOfChildAccount property value
        return Stream.of(
            arguments(2, 0), // property is omitted
            arguments(5, 0),
            arguments(6, 0),
            arguments(9, 0),
            arguments(15, 1),// property is specified
            arguments(16, 1),
            arguments(19, 1),
            arguments(23, 2),
            arguments(30, 3),
            arguments(31, 3),
            arguments(39, 3),
            arguments(40, 4),
            arguments(50, 4),
            arguments(1025, 4),
            arguments(Integer.MAX_VALUE, 4)
        );
    }

    private void prepareAndInitComponents() {
        chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
        assertNotNull(chain);
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, holder);
    }

}