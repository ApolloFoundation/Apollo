/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ChainsConfigLoaderTest {
    private static UUID chainId = UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6");
    private static final List<BlockchainProperties> BLOCKCHAIN_PROPERTIES = Arrays.asList(
        new BlockchainProperties(0, 255, 60, 67, 53, 30000000000L),
            new BlockchainProperties(2000, 300, 2, 4, 1,  30000000000L, new ConsensusSettings(ConsensusSettings.Type.POS,
                    new AdaptiveForgingSettings(true, 60, 0))),
            new BlockchainProperties(42300, 300, 2, 4, 1, 30000000000L, new ShardingSettings(true), new ConsensusSettings(new AdaptiveForgingSettings(true, 10, 0))),
            new BlockchainProperties(100000, 300, 2, 4, 1, 30000000000L, new ShardingSettings(true, 1_000_000),
                    new ConsensusSettings(new AdaptiveForgingSettings(true, 10, 0)))
    );

    private static final Chain EXPECTED_CHAIN = new Chain(chainId, true, Collections.emptyList(), Arrays.asList("51.15.250.32",
            "51.15.253.171",
            "51.15.210.116",
            "51.15.242.197",
            "51.15.218.241"), Collections.emptyList(), "Apollo experimental testnet", "NOT STABLE testnet for experiments. Don't use it if you " +
            "don't know what is it", "Apollo", "APL", "Apollo", "data/genesisAccounts-testnet.json", BLOCKCHAIN_PROPERTIES);
    @Mock
    private ConfigDirProvider configDirProvider;

    @Test
    void testLoadConfig() {
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(configDirProvider, false, "conf", "test-chains.json");
        Map<UUID, Chain> chains = chainsConfigLoader.load();
        Assertions.assertEquals(1, chains.size());
        Chain actualChain = chains.get(chainId);
        Assertions.assertNotNull(actualChain);
        Assertions.assertEquals(EXPECTED_CHAIN, actualChain);
    }

}
