/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.config.SmcSettings;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    PropertiesHolder holder = mock(PropertiesHolder.class);

    @Test
    void copy() {
        //GIVEN
        var cfg1 = new SmcSettings("1234567890");
        //WHEN
        var cfg2 = cfg1.copy();
        //THEN
        assertEquals(cfg1, cfg2);
    }

    @Test
    void testMasterAccountPKDefaultValue() {
        //GIVEN
        //WHEM
        var cfg = new SmcSettings();
        //THEN
        assertNull(cfg.getMasterAccountPK());
    }

    @CsvSource({
        "1,aa8a4621988974e669d115b5e0d48234eed145ff2ac94ec1012805459470fe1e",
        "3,aa8a4621988974e669d115b5e0d48234eed145ff2ac94ec1012805459470fe1e",
        "5,bb8a4621988974e669d115b5e0d48234eed145ff2ac94ec1012805459470fe1e",
        "9,bb8a4621988974e669d115b5e0d48234eed145ff2ac94ec1012805459470fe1e",
        "11,",
        "50,",
        "500,cc8a4621988974e669d115b5e0d48234eed145ff2ac94ec1012805459470fe1e",
        "999,cc8a4621988974e669d115b5e0d48234eed145ff2ac94ec1012805459470fe1e",
        "1000,",
        "20000,"})
    @ParameterizedTest
    void test_atHeight_AllCorrectConfigs(int height, String pk) {
        //GIVEN
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        Map<UUID, Chain> loadedChains = chainsConfigLoader.load();
        doReturn(5000).when(holder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(holder).getIntProperty("apl.minPrunableLifetime");

        Chain chain = loadedChains.get(CHAIN_UUID);
        BlockchainConfig blockchainConfig = new BlockchainConfig(chain, holder);
        //WHEN
        HeightConfig result = blockchainConfig.getConfigAtHeight(height);  // checked method
        log.trace("result = {}", result);
        //THEN
        assertNotNull(result);
        assertArrayEquals(Convert.parseHexString(pk), result.getSmcMasterAccountPublicKey());
    }
}