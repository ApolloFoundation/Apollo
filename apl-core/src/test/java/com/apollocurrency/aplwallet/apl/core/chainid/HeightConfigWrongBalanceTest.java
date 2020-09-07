/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

@Slf4j
class HeightConfigWrongBalanceTest {

    // config is used for simulating complex shard settings
    private static final String CONFIG_NAME_WITH_WRONG_BALANCE = "wrong-max-balance-config.json";
    private PropertiesHolder holder = mock(PropertiesHolder.class);

    @BeforeEach
    void setup() {
    }

    @Test
    void test_atHeight_WrongBalance() {
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME_WITH_WRONG_BALANCE);
        Map<UUID, Chain> loadedChains = chainsConfigLoader.load();
        try {
            Chain chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
            assertNotNull(chain);
            assertNotNull(chain.getBlockchainProperties());
            new BlockchainConfig(chain, holder);

            fail("Unexpected flow.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Wrong height config, height="));
        }
    }

}