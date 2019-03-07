/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import javax.inject.Inject;

import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
@EnableWeld
public class BlockchainConfigTest {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.from(BlockchainProcessorImpl.class, BlockchainConfig.class,
            DefaultBlockValidator.class, BlockDaoImpl.class, BlockchainConfigUpdater.class, PropertiesHolder.class,
            EpochTime.class, NtpTime.class)
            .build();

    private static final BlockchainProperties bp1 = new BlockchainProperties(0, 0, 1, 0, 0, 100L);
    private static final BlockchainProperties bp2 = new BlockchainProperties(100, 0, 1, 0, 0, 100L);
    private static final BlockchainProperties bp3 = new BlockchainProperties(200, 0, 2, 0, 0, 100L);
    private static final List<BlockchainProperties> BLOCKCHAIN_PROPERTIES = Arrays.asList(
            bp1,
            bp2,
            bp3
    );

    @Inject
    private BlockchainConfig blockchainConfig;
    @Inject
    private BlockchainConfigUpdater blockchainConfigUpdater;

    private static final Chain chain = new Chain(UUID.randomUUID(), true, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(),
            "test",
            "test",
            "TEST",
            "TEST", "Test", "data.json", BLOCKCHAIN_PROPERTIES);

    @Test
    public void testInitBlockchainConfig() {
        blockchainConfig.updateChain(chain);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testUpdateBlockchainConfig() {
        blockchainConfigUpdater.updateChain(chain);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testUpdateToHeight() {
        blockchainConfigUpdater.updateChain(chain);
        blockchainConfigUpdater.updateToHeight(99, true);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(100, true);
        Assertions.assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(100, false);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(201, true);
        Assertions.assertEquals(new HeightConfig(bp3), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(0, false);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(0, true);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testRollback() {
        blockchainConfigUpdater.updateChain(chain);
        blockchainConfigUpdater.updateToHeight(102, true);
        Assertions.assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.rollback(100);
        Assertions.assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.rollback(99);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }
}
