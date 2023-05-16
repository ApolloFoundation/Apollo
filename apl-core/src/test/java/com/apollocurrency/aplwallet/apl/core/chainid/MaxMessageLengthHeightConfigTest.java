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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
class MaxMessageLengthHeightConfigTest {

    // config is used for simulating complex shard settings
    private static final String CONFIG_NAME = "max-arbitrary-message-length-config.json";
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
    @MethodSource("provideHeightAndMaxLength")
    void test_atHeight_AllCorrectConfigsWithFrequency(int height, int length) {
        HeightConfig result = blockchainConfig.getConfigAtHeight(height);  // checked method
        log.trace("result = {}", result);
        assertNotNull(result);
        assertEquals(length, result.getMaxArbitraryMessageLength());
    }

    /**
     * Height and target Max Arbitrary message length are supplied into unit test method
     *
     * @return height + length value for test
     */
    static Stream<Arguments> provideHeightAndMaxLength() {
        return Stream.of(
            arguments(0, 160),//not set, but 160 is min default value
            arguments(1, 180),//set in config
            arguments(10, 1000),//set in config
            arguments(11, 1000),
            arguments(20, 2000),//set in config
            arguments(22, 2000),
            arguments(1025, 2000),
            arguments(Integer.MAX_VALUE, 2000)
        );
    }

    private void prepareAndInitComponents() {
        chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
        assertNotNull(chain);
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, holder);
    }

}