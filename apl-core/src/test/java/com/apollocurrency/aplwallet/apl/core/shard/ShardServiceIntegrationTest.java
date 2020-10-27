/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.ShardTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.MariaDBContainer;

import javax.enterprise.event.Event;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j

@Tag("slow")
@ExtendWith(MockitoExtension.class)
public class ShardServiceIntegrationTest extends DbContainerBaseTest {

    @RegisterExtension
    TemporaryFolderExtension folder = new TemporaryFolderExtension();
    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);
    ShardService shardService;
    @Mock
    BlockchainProcessor blockchainProcessor;
    @Mock
    Blockchain blockchain;
    @Mock
    DirProvider dirProvider;
    @Mock
    TrimService trimService;
    Zip zip = new ZipImpl();
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    ShardRecoveryDao shardRecoveryDao;
    @Mock
    ShardMigrationExecutor shardMigrationExecutor;
    @Mock
    AplAppStatus aplAppStatus;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    Event<TrimConfig> trimEvent;
    @Mock
    Event<DbHotSwapConfig> dbEvent;
    @Mock
    GlobalSync globalSync;

    private ShardDao createShardDao() {
        JdbiHandleFactory jdbiHandleFactory = new JdbiHandleFactory();
        jdbiHandleFactory.setJdbi(extension.getDatabaseManager().getJdbi());
        return JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(jdbiHandleFactory, ShardDao.class);
    }

    @Test
    void testGetAllShards() {
        shardService = new ShardService(createShardDao(), blockchainProcessor, blockchain, dirProvider, zip, extension.getDatabaseManager(), blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync, trimService, dbEvent);
        List<Shard> allShards = shardService.getAllShards();

        assertEquals(ShardTestData.SHARDS, allShards);
    }

    @Test
    void testGetAllCompletedShards() {
        shardService = new ShardService(createShardDao(), blockchainProcessor, blockchain, dirProvider, zip, extension.getDatabaseManager(), blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync, trimService, dbEvent);
        List<Shard> allShards = shardService.getAllCompletedShards();

        assertEquals(List.of(ShardTestData.SHARD_1), allShards);
    }

    @Test
    void testGetAllCompletedOrArchivedShards() {
        shardService = new ShardService(createShardDao(), blockchainProcessor, blockchain, dirProvider, zip, extension.getDatabaseManager(), blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync, trimService, dbEvent);

        List<Shard> shards = shardService.getAllCompletedOrArchivedShards();

        assertEquals(List.of(ShardTestData.SHARD_2, ShardTestData.SHARD_1), shards);
    }

    @Test
    void testReset() throws IOException, SQLException {
//        Path dbDir = folder.newFolder().toPath();
//        DatabaseManagerImpl databaseManager = new DatabaseManagerImpl(DbTestData.getDbFilePropertiesByPath(dbDir.resolve(Constants.APPLICATION_DIR_NAME)), new PropertiesHolder(), new JdbiHandleFactory());
        DbProperties dbProperties = DbTestData.getInMemDbProps();
        if (mariaDBContainer.getMappedPort(3306) != null) {
            dbProperties.setDatabasePort(mariaDBContainer.getMappedPort(3306));
        }
        dbProperties.setDatabaseHost(mariaDBContainer.getHost());
        dbProperties.setDbName(((MariaDBContainer<?>) mariaDBContainer).getDatabaseName());
        DatabaseManagerImpl databaseManager = new DatabaseManagerImpl(dbProperties, new PropertiesHolder(), new JdbiHandleFactory());
//        Chain mockChain = mock(Chain.class);
//        doReturn(mockChain).when(blockchainConfig).getChain();
//        doReturn(UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5")).when(mockChain).getChainId();
//        Event firedEvent = mock(Event.class);
//        doReturn(firedEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {
//        });
        shardService = new ShardService(createShardDao(databaseManager.getJdbiHandleFactory()), blockchainProcessor, blockchain, dirProvider, zip, databaseManager, blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync, trimService, dbEvent);

//        Files.createFile(dbDir.resolve("apl-blockchain-shard-2-chain." + DbProperties.DB_EXTENSION)); // to be deleted
//        Files.createFile(dbDir.resolve("apl-blockchain-shard-1-chain." + DbProperties.DB_EXTENSION)); // to be replaced
//        Files.createFile(dbDir.resolve("apl-blockchain-shard-0-chain." + DbProperties.DB_EXTENSION)); // to be saved
//        Path backupDir = dbDir.resolve("backup");
//        Files.createDirectory(backupDir);

//        Path dbPath = backupDir.resolve(Constants.APPLICATION_DIR_NAME);
//        DbManipulator manipulator = new DbManipulator(dbProperties, null, "db/shard/service-reset-data.sql", "db/schema.sql");
//        manipulator.init();
//        manipulator.populate();
//        manipulator.shutdown();

//        doReturn(dbDir).when(dirProvider).getDbDir();
//        doReturn(mock(HeightConfig.class)).when(blockchainConfig).getCurrentConfig();

//        TransactionalDataSource shardDatasource = databaseManager.getOrCreateShardDataSourceById(1L);
//        databaseManager.getDataSource().begin();

//        Path zipPath = dbDir.resolve("BACKUP-BEFORE-apl-blockchain-shard-1-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip");
//        zip.compress(zipPath.toAbsolutePath().toString(), dbPath.getParent().toAbsolutePath().toString(), 0L, null, false);

//        boolean reset = shardService.reset(1);
//        assertTrue(reset);

        assertThrows(IllegalStateException.class, () -> databaseManager.getDataSource().commit()); //previous datasource was closed

//        assertThrows(SQLException.class, shardDatasource::getConnection); //shard datasource was closed

//        assertEquals(4, FileUtils.countElementsOfDirectory(dbDir, (dir) -> dir.toFile().isFile())); // files only
//        Files.exists(zipPath);
//        Files.exists(backupDir);
//        Files.exists(dbDir.resolve(Constants.APPLICATION_DIR_NAME + DbProperties.DB_EXTENSION_WITH_DOT));
        List<Shard> allShards = shardService.getAllShards();
        assertEquals(3, allShards.size());
        Shard shard = allShards.get(0);
//        assertEquals(1000, shard.getShardHeight());
        assertEquals(2, shard.getShardHeight());
//        assertEquals(ShardState.FULL, shard.getShardState());
        assertEquals(ShardState.INIT, shard.getShardState());
//        verify(globalSync).writeLock();
//        verify(globalSync).writeUnlock();
//        verify(dbEvent).fire(new DbHotSwapConfig(1));
//        verify(firedEvent).fire(new TrimConfig(false, true));
//        verify(firedEvent).fire(new TrimConfig(true, false));
//        verify(blockchainProcessor).suspendBlockchainDownloading();
//        verify(blockchainProcessor).resumeBlockchainDownloading();
    }

    private ShardDao createShardDao(JdbiHandleFactory jdbiHandleFactory) {
        jdbiHandleFactory.setJdbi(extension.getDatabaseManager().getJdbi());
        return JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(jdbiHandleFactory, ShardDao.class);
    }

}
