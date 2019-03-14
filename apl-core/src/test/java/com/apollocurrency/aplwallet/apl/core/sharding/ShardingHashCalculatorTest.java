/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbExtension;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.io.IOException;
import javax.inject.Inject;

@EnableWeld

class ShardingHashCalculatorTest {
    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
    DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
    HeightConfig heightConfig = Mockito.mock(HeightConfig.class);

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(BlockchainImpl.class, BlockDaoImpl.class, DerivedDbTablesRegistry.class, EpochTime.class, GlobalSyncImpl.class)
            .addBeans(
                    MockBean.of(blockchainConfig, BlockchainConfig.class),
                    MockBean.of(propertiesHolder, PropertiesHolder.class),
                    MockBean.of(databaseManager, DatabaseManager.class)
            ).build();
    @RegisterExtension
    DbExtension dbExtension = new DbExtension();
    @Inject
    Blockchain blockchain;

    @Test
    public void testCalculateHash() throws IOException {

        Mockito.doReturn("SHA-512").when(heightConfig).getShardingDigestAlgorithm();
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        ShardingHashCalculator shardingHashCalculator = new ShardingHashCalculator(blockchain, blockchainConfig, 2);

    }
}
