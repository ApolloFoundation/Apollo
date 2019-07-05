/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.data.ShardTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import javax.enterprise.event.Event;
@ExtendWith(MockitoExtension.class)
public class ShardServiceIntegrationTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    ShardService shardService;
    @Mock
    BlockchainProcessor blockchainProcessor;
    @Mock
    Blockchain blockchain;
    @Mock
    DirProvider dirProvider;
    @Mock
    Zip zip;
    @Mock
    DatabaseManager databaseManager;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    ShardRecoveryDao shardRecoveryDao;
    @Mock ShardMigrationExecutor shardMigrationExecutor;
    @Mock
    AplAppStatus aplAppStatus;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    Event<Boolean> trimEvent;
    @Mock
    GlobalSync globalSync;
    @BeforeEach
    void setUp() {
        JdbiHandleFactory jdbiHandleFactory = new JdbiHandleFactory();
        jdbiHandleFactory.setJdbi(extension.getDatabaseManager().getJdbi());
        ShardDao shardDao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(jdbiHandleFactory, ShardDao.class);
        shardService = new ShardService(shardDao, blockchainProcessor, blockchain, dirProvider, zip, databaseManager, blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync);
    }


    @Test
    void testGetAllShards() {
        List<Shard> allShards = shardService.getAllShards();

        assertEquals(ShardTestData.SHARDS, allShards);
    }
}
