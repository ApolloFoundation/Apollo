/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.SynchronizationServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

@EnableWeld
public class ShardingHashCalculatorTest {
    DatabaseManager databaseManager = new DatabaseManager(DbTestData.getDbFileProperties("/home/user/.apl-blockchain/apl-blockchain-db/b5d7b6/apl" +
            "-blockchain"), new PropertiesHolder());
    @WeldSetup
    WeldInitiator weld =
            WeldInitiator.from(BlockchainImpl.class, BlockDaoImpl.class, DerivedDbTablesRegistry.class,
                    TransactionDaoImpl.class, PropertiesHolder.class, EpochTime.class, BlockchainConfig.class,
                    NtpTime.class, SynchronizationServiceImpl.class, ShardingHashCalculator.class).addBeans(MockBean.of(databaseManager,
                    DatabaseManager.class)).build();

    @Inject
    ShardingHashCalculator shardingHashCalculator;
    @Inject
    Blockchain blockchain;
    @Test
    public void testCalculateHash() {
        Block block = Mockito.mock(Block.class);
        Mockito.doReturn(1_800_000).when(block).getHeight();
        blockchain.setLastBlock(block);
        byte[] bytes = shardingHashCalculator.calculateHash(0);
    }

}
