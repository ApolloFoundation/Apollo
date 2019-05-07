/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.MAIN_DB_BACKUPED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.DATA_TAG_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PRUNABLE_MESSAGE_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.SHUFFLING_DATA_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_SHARD_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_TABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfoImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.UserMode;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

@EnableWeld
class DataTransferManagementReceiverTest {
    private static final Logger log = getLogger(DataTransferManagementReceiverTest.class);

/*
  // YL  DO NOT REMOVE THAT PLEASE, it can be used for manual testing
    private static String BASE_SUB_DIR = "unit-test-db";
    private static Path pathToDb = FileSystems.getDefault().getPath(System.getProperty("user.dir") + File.separator  + BASE_SUB_DIR);;
    @RegisterExtension
    DbExtension extension = new DbExtension(baseDbProperties, propertiesHolder);
    private static DbProperties baseDbProperties;
    private static PropertiesHolder propertiesHolder;
*/

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("targetDb").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();


    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ReferencedTransactionDao.class, ShardDao.class, ShardRecoveryDao.class,
            DerivedDbTablesRegistryImpl.class,
            TransactionTestData.class, PropertyProducer.class, ShardRecoveryDaoJdbcImpl.class,
            GlobalSyncImpl.class, FullTextConfigImpl.class, FullTextConfig.class,
            DataTransferManagementReceiverImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class, TrimService.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
//            .addBeans(MockBean.of(baseDbProperties, DbProperties.class)) // YL  DO NOT REMOVE THAT PLEASE, it can be used for manual testing
            .build();

    @Inject
    private JdbiHandleFactory jdbiHandleFactory;
    @Inject
    private DataTransferManagementReceiver managementReceiver;
    @Inject
    private BlockIndexDao blockIndexDao;
    @Inject
    private TransactionIndexDao transactionIndexDao;
    @Inject
    private BlockDao blockDao;
    @Inject
    private ShardDao shardDao;
    @Inject
    private ShardRecoveryDao recoveryDao;

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

/*
    // YL  DO NOT REMOVE THAT PLEASE, it can be used for manual testing
    @BeforeAll
    static void setUpAll() {
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                null,
                false,
                null,
                Constants.APPLICATION_DIR_NAME + ".properties",
                Collections.emptyList());
        propertiesHolder = new PropertiesHolder();
        Properties properties = propertiesLoader.load();
        properties.setProperty("apl.testData", "false");// true - load data.sql, false - do not load data.sql
        propertiesHolder.init(properties);
        DbConfig dbConfig = new DbConfig(propertiesHolder);
        baseDbProperties = dbConfig.getDbConfig();
    }
*/

    @AfterEach
    void tearDown() {
        jdbiHandleFactory.close();
/*
        // YL  DO NOT REMOVE THAT PLEASE, it can be used for manual testing
        extension.getDatabaseManger().shutdown();
        FileUtils.deleteQuietly(pathToDb.toFile()); // remove after every test
*/
    }

    @Test
    void createShardDb() throws IOException {
        MigrateState state = managementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);
        state = managementReceiver.addOrCreateShard(new ShardInitTableSchemaVersion());
        assertEquals(SHARD_SCHEMA_CREATED, state);
    }

    @Test
    void createFullShardDb() throws IOException {
        MigrateState state = managementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        state = managementReceiver.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
        assertEquals(SHARD_SCHEMA_FULL, state);
    }

    @Test
    void createShardDbDoAllOperations() throws IOException {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("backup").toPath()).when(dirProvider).getDbDir();
        AplCoreRuntime.getInstance().setup(new UserMode(), dirProvider);
        try { //AplCoreRuntime will be loaded we should setUp to null values for another tests
            long start = System.currentTimeMillis();
            MigrateState state = managementReceiver.getCurrentState();
            assertNotNull(state);
            assertEquals(MigrateState.INIT, state);

            int snapshotBlockHeight = 8000;

            // prepare an save Recovery + new Shard info
            ShardRecovery recovery = new ShardRecovery(state);
            recoveryDao.saveShardRecovery(recovery);
            byte[] shardHash = "000000000".getBytes();
            Shard newShard = new Shard(shardHash, snapshotBlockHeight);
            shardDao.saveShard(newShard);

            state = managementReceiver.createBackup();
            assertEquals(MAIN_DB_BACKUPED, state);
            assertTrue(Files.exists(dirProvider.getDbDir().resolve("BACKUP-BEFORE-apl-blockchain-shard-0000004.zip")));

            // start sharding process
            state = managementReceiver.addOrCreateShard(new ShardInitTableSchemaVersion());
            assertEquals(SHARD_SCHEMA_CREATED, state);

            List<String> tableNameList = new ArrayList<>();
            tableNameList.add(BLOCK_TABLE_NAME);
            tableNameList.add(TRANSACTION_TABLE_NAME);
            TransactionTestData td = new TransactionTestData();
            Set<Long> dbIds = new HashSet<>();
            dbIds.add(td.DB_ID_0);
            dbIds.add(td.DB_ID_3);
            dbIds.add(td.DB_ID_10);
            CommandParamInfo paramInfo = new CommandParamInfoImpl(tableNameList, 2, snapshotBlockHeight, dbIds);

            state = managementReceiver.copyDataToShard(paramInfo);
            assertEquals(MigrateState.DATA_COPIED_TO_SHARD, state);
//        assertEquals(MigrateState.FAILED, state);

            TransactionalDataSource shardDataSource = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(4L);
            long count = blockDao.getBlockCount(shardDataSource, 0, (int) snapshotBlockHeight);
            assertEquals(8, count);

            state = managementReceiver.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
            assertEquals(SHARD_SCHEMA_FULL, state);

            tableNameList.clear();
            tableNameList.add(PUBLIC_KEY_TABLE_NAME);
//        tableNameList.add(TAGGED_DATA_TABLE_NAME); // ! skip in test
            tableNameList.add(SHUFFLING_DATA_TABLE_NAME);
            tableNameList.add(DATA_TAG_TABLE_NAME);
            tableNameList.add(PRUNABLE_MESSAGE_TABLE_NAME);

//            paramInfo.setTableNameList(tableNameList);
//            state = managementReceiver.relinkDataToSnapshotBlock(paramInfo);
//            assertEquals(MigrateState.DATA_RELINKED_IN_MAIN, state);
//        assertEquals(MigrateState.FAILED, state);

            tableNameList.clear();
            tableNameList.add(BLOCK_INDEX_TABLE_NAME);
            tableNameList.add(TRANSACTION_SHARD_INDEX_TABLE_NAME);

            paramInfo.setTableNameList(tableNameList);
            state = managementReceiver.updateSecondaryIndex(paramInfo);
            assertEquals(MigrateState.SECONDARY_INDEX_UPDATED, state);
//        assertEquals(MigrateState.FAILED, state);

            long blockIndexCount = blockIndexDao.countBlockIndexByShard(4L);
            assertEquals(8, blockIndexCount);
            long trIndexCount = transactionIndexDao.countTransactionIndexByShardId(4L);
            assertEquals(5, trIndexCount);

            tableNameList.clear();
            tableNameList.add(BLOCK_TABLE_NAME);

            paramInfo.setTableNameList(tableNameList);
            state = managementReceiver.deleteCopiedData(paramInfo);
            assertEquals(MigrateState.DATA_REMOVED_FROM_MAIN, state);
//        assertEquals(MigrateState.FAILED, state);

            count = blockDao.getBlockCount((int) snapshotBlockHeight, 105000);
            assertEquals(5, count);

            paramInfo.setShardHash(shardHash);
            state = managementReceiver.addShardInfo(paramInfo);
            assertEquals(MigrateState.COMPLETED, state);

// compare fullhashes
            TransactionIndex index = transactionIndexDao.getByTransactionId(td.TRANSACTION_1.getId());
            assertNotNull(index);
            byte[] fullHash = Convert.toFullHash(index.getTransactionId(), index.getPartialTransactionHash());
            assertArrayEquals(td.TRANSACTION_1.getFullHash(), fullHash);
            log.debug("Migration finished in = {} sec", (System.currentTimeMillis() - start) / 1000);
        }
        finally {
            AplCoreRuntime.getInstance().setup(null, null);
        }
    }
}