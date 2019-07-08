/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.ShardTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.enterprise.event.Event;
@ExtendWith(MockitoExtension.class)
public class ShardServiceIntegrationTest {
    @RegisterExtension
    TemporaryFolderExtension folder = new TemporaryFolderExtension();
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

    private ShardDao createShardDao() {
        JdbiHandleFactory jdbiHandleFactory = new JdbiHandleFactory();
        jdbiHandleFactory.setJdbi(extension.getDatabaseManager().getJdbi());
        return JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(jdbiHandleFactory, ShardDao.class);
    }

    @Test
    void testGetAllShards() {
        shardService = new ShardService(createShardDao(), blockchainProcessor, blockchain, dirProvider, zip, extension.getDatabaseManager(), blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync);
        List<Shard> allShards = shardService.getAllShards();

        assertEquals(ShardTestData.SHARDS, allShards);
    }

    @Test
    void testGetAllCompletedShards() {
        shardService = new ShardService(createShardDao(), blockchainProcessor, blockchain, dirProvider, zip, extension.getDatabaseManager(), blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync);
        List<Shard> allShards = shardService.getAllCompletedShards();

        assertEquals(List.of(ShardTestData.SHARD_1), allShards);
    }

    @Test
    void testReset() throws IOException, SQLException {
        Path dbDir = folder.newFolder().toPath();
        DatabaseManagerImpl databaseManager = new DatabaseManagerImpl(DbTestData.getDbFileProperties(dbDir.resolve(Constants.APPLICATION_DIR_NAME)), new PropertiesHolder(), new JdbiHandleFactory());
        Chain mockChain = mock(Chain.class);
        doReturn(mockChain).when(blockchainConfig).getChain();
        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(mockChain).getChainId();
        zip = new ZipImpl();
        shardService = new ShardService(createShardDao(databaseManager.getJdbiHandleFactory()), blockchainProcessor, blockchain, dirProvider, zip, databaseManager, blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync);
        Files.createFile(dbDir.resolve("apl-blockchain-shard-2-chain.h2.db"));
        Files.createFile(dbDir.resolve("apl-blockchain-shard-1-chain.h2.db"));
        Path backupDir = dbDir.resolve("backup");
        Files.createDirectory(backupDir);
        Path dbPath = backupDir.resolve(Constants.APPLICATION_DIR_NAME);
        DbManipulator manipulator = new DbManipulator(DbTestData.getDbFileProperties(dbPath), null, "db/shard/service-reset-data.sql", "db/schema.sql");
        manipulator.init();
        manipulator.populate();
        manipulator.shutdown();
        doReturn(dbDir).when(dirProvider).getDbDir();
        doReturn(mock(HeightConfig.class)).when(blockchainConfig).getCurrentConfig();
        TransactionalDataSource shardDatasource = databaseManager.getOrCreateShardDataSourceById(1L);
        shardDatasource.begin();
        databaseManager.getDataSource().begin();

        Path zipPath = dbDir.resolve("BACKUP-BEFORE-apl-blockchain-shard-1-chain-a2e9b946-290b-48b6-9985-dc2e5a5860a1.zip");
        zip.compress(zipPath.toAbsolutePath().toString(), dbPath.getParent().toAbsolutePath().toString(), 0L, null, false);

        boolean reset = shardService.reset(1);
        assertTrue(reset);
        assertThrows(IllegalStateException.class, () -> databaseManager.getDataSource().commit()); //previous datasource was closed
        assertThrows(RuntimeException.class, () -> shardDatasource.commit()); //shard datasource was closed
        long files = Files.list(dbDir).count();
        assertEquals(5, files);
        Files.exists(zipPath);
        Files.exists(backupDir);
        Files.exists(dbDir.resolve(Constants.APPLICATION_DIR_NAME + ".h2.db"));
        List<Shard> allShards = shardService.getAllShards();
        assertEquals(1, allShards.size());
        Shard shard = allShards.get(0);
        assertEquals(1000, shard.getShardHeight());
        assertEquals(100, shard.getShardState());
    }

    private ShardDao createShardDao(JdbiHandleFactory jdbiHandleFactory) {
        jdbiHandleFactory.setJdbi(extension.getDatabaseManager().getJdbi());
        return JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(jdbiHandleFactory, ShardDao.class);
    }

}
