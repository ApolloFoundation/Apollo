/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
class SmcSettingsTest {
    // config is used for simulating complex shard settings
    private static final String CONFIG_NAME = "chains-smc-config.json";
    private static final UUID CHAIN_UUID = UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6");
    private PropertiesHolder holder = mock(PropertiesHolder.class);
    private Map<UUID, Chain> loadedChains;
    private BlockchainConfig blockchainConfig;
    private Chain chain;

    @BeforeEach
    void setUp() {
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        loadedChains = chainsConfigLoader.load();
        assertEquals(1, loadedChains.size());
        assertNotNull(loadedChains.get(CHAIN_UUID));
        doReturn(5000).when(holder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(holder).getIntProperty("apl.minPrunableLifetime");

        prepareAndInitComponents();
    }

    @ParameterizedTest
    @CsvSource({"1, -1234567890", "3, -1234567890", "5,-1111111111", "50,-1111111111", "500,-2222222222", "1000,-2222222222", "20000,-2222222222"})
    void test_atHeight_AllCorrectConfigs(int height, long accountId) {

        HeightConfig result = blockchainConfig.getConfigAtHeight(height);  // checked method
        log.trace("result = {}", result);
        assertNotNull(result);
        assertEquals(accountId, result.getSmcMasterAccountId());
    }

    @Test
    void copy() {
    }

    @Test
    void getMasterAccountId() {
    }

    private void prepareAndInitComponents() {
        chain = loadedChains.get(CHAIN_UUID);
        assertNotNull(chain);
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, holder);
    }
}