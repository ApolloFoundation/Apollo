/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.MAIN_DB_BACKUPED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.SHARD_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_SHARD_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_12_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbc;
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
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfoImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporterImpl;
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
import org.jboss.weld.literal.NamedLiteral;
import org.jdbi.v3.core.Jdbi;
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
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import org.junit.jupiter.api.Disabled;

@EnableWeld
class ShardEngineTest {
    private static final Logger log = getLogger(ShardEngineTest.class);

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

    private final Bean<Path> dataExportDir = MockBean.of(createPath("targetDb").toAbsolutePath(), Path.class);
    {
        dataExportDir.getQualifiers().add(new NamedLiteral("dataExportDir"));
    }
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ReferencedTransactionDao.class, ShardDao.class, ShardRecoveryDao.class,
            DerivedDbTablesRegistryImpl.class,
            TransactionTestData.class, PropertyProducer.class, ShardRecoveryDaoJdbcImpl.class,
            GlobalSyncImpl.class, FullTextConfigImpl.class, FullTextConfig.class,
            DerivedTablesRegistry.class,
            ShardEngineImpl.class, CsvExporterImpl.class, ShardDaoJdbcImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class, TrimService.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(dataExportDir)
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
//            .addBeans(MockBean.of(baseDbProperties, DbProperties.class)) // YL  DO NOT REMOVE THAT PLEASE, it can be used for manual testing
            .build();

    @Inject
    private ShardEngine shardEngine;
    @Inject
    private BlockIndexDao blockIndexDao;
    @Inject
    private TransactionIndexDao transactionIndexDao;
    @Inject
    private Blockchain blockchain;
    @Inject
    private ShardDao shardDao;
    @Inject
    private ShardRecoveryDaoJdbc shardRecoveryDaoJdbc;
    @Inject
    private ShardRecoveryDaoJdbc recoveryDao;
    @Inject
    private DerivedTablesRegistry registry;
    @Inject
    private CsvExporter cvsExporter;

    public ShardEngineTest() throws Exception {}

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


    @Test
    void createShardDb() throws IOException {
        MigrateState state = shardEngine.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);
        state = shardEngine.addOrCreateShard(new ShardInitTableSchemaVersion());
        assertEquals(SHARD_SCHEMA_CREATED, state);
    }

    @Test
    void createFullShardDb() throws IOException {
        MigrateState state = shardEngine.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        state = shardEngine.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
        assertEquals(SHARD_SCHEMA_FULL, state);
    }

    @Test           
    void createShardDbDoAllOperations() throws IOException {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("backup").toPath()).when(dirProvider).getDbDir();
//TODO: do we need entire Apl Core here ?        
//        AplCoreRuntime.getInstance().setup(new UserMode(), dirProvider);
        blockIndexDao.hardDeleteAllBlockIndex();
        try { //AplCoreRuntime will be loaded we should setUp to null values for another tests
            long start = System.currentTimeMillis();
            MigrateState state = shardEngine.getCurrentState();
            assertNotNull(state);
            assertEquals(MigrateState.INIT, state);

            int snapshotBlockHeight = 8000;

            // prepare and save Recovery + new Shard info
            ShardRecovery recovery = new ShardRecovery(state);
            recoveryDao.saveShardRecovery(extension.getDatabaseManger().getDataSource(), recovery);
            byte[] shardHash = "000000000".getBytes();
            Shard newShard = new Shard(shardHash, snapshotBlockHeight);
            shardDao.saveShard(newShard);

//1.        // create main db backup
            state = shardEngine.createBackup();
            assertEquals(MAIN_DB_BACKUPED, state);
            assertTrue(Files.exists(dirProvider.getDbDir().resolve("BACKUP-BEFORE-apl-blockchain-shard-4.zip")));

//2.        // create shard db with 'initial' schema
            state = shardEngine.addOrCreateShard(new ShardInitTableSchemaVersion());
            assertEquals(SHARD_SCHEMA_CREATED, state);

            // checks before COPYING blocks / transactions
            long count = blockchain.getBlockCount(null, 0, BLOCK_12_HEIGHT + 1); // upper bound is excluded, so +1
            assertEquals(14, count); // total blocks in main db
            count = blockchain.getTransactionCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
            assertEquals(14, count); // total transactions in main db

            List<String> tableNameList = new ArrayList<>();
            tableNameList.add(BLOCK_TABLE_NAME);
            tableNameList.add(TRANSACTION_TABLE_NAME);
            TransactionTestData td = new TransactionTestData();
            Set<Long> dbIds = new HashSet<>();
            dbIds.add(td.DB_ID_0);
            dbIds.add(td.DB_ID_2);
            dbIds.add(td.DB_ID_5);
            CommandParamInfo paramInfo = new CommandParamInfoImpl(tableNameList, 2, snapshotBlockHeight, dbIds);

//3-4.      // copy block + transaction data from main db into shard
            state = shardEngine.copyDataToShard(paramInfo);
            assertEquals(MigrateState.DATA_COPY_TO_SHARD_FINISHED, state);
//        assertEquals(MigrateState.FAILED, state);

            // check after COPY
            TransactionalDataSource shardDataSource = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getBlockCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(8, count); // blocks in shard db
            shardDataSource = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getTransactionCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(4, count);// transactions in shard db

//5.        // create shard db FULL schema
            state = shardEngine.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
            assertEquals(SHARD_SCHEMA_FULL, state);

//            tableNameList.clear();
//            tableNameList.add(PUBLIC_KEY_TABLE_NAME);
//            tableNameList.add(TAGGED_DATA_TABLE_NAME);
//            tableNameList.add(SHUFFLING_DATA_TABLE_NAME);
//            tableNameList.add(DATA_TAG_TABLE_NAME);
//            tableNameList.add(PRUNABLE_MESSAGE_TABLE_NAME);
//            paramInfo.setTableNameList(tableNameList);
//            state = shardEngine.relinkDataToSnapshotBlock(paramInfo);
//            assertEquals(MigrateState.DATA_RELINKED_IN_MAIN, state);
//        assertEquals(MigrateState.FAILED, state);

            tableNameList.clear();
            tableNameList.add(BLOCK_INDEX_TABLE_NAME);
            tableNameList.add(TRANSACTION_SHARD_INDEX_TABLE_NAME);
            paramInfo.setTableNameList(tableNameList);

//6-7.      // update secondary block + transaction indexes
            state = shardEngine.updateSecondaryIndex(paramInfo);
            assertEquals(MigrateState.SECONDARY_INDEX_FINISHED, state);
//        assertEquals(MigrateState.FAILED, state);

            long blockIndexCount = blockIndexDao.countBlockIndexByShard(4L);
            // should be 8 but prev shard already exist and grabbed our genesis block
            assertEquals(7, blockIndexCount);
            long trIndexCount = transactionIndexDao.countTransactionIndexByShardId(4L);
            assertEquals(4, trIndexCount);


            tableNameList.clear();
            tableNameList.add(SHARD_TABLE_NAME);
            tableNameList.add(BLOCK_INDEX_TABLE_NAME);
            tableNameList.add(TRANSACTION_SHARD_INDEX_TABLE_NAME);
            paramInfo.setTableNameList(tableNameList);
//8-9.      // export 'derived', shard, secondary block + transaction indexes
            state = shardEngine.exportCsv(paramInfo);
            assertEquals(MigrateState.CSV_EXPORT_FINISHED, state);

            tableNameList.clear();
//10-11.    // archive CSV into zip
            state = shardEngine.archiveCsv(paramInfo);
            assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, state);
//        assertEquals(MigrateState.FAILED, state);

            tableNameList.clear();
            tableNameList.add(BLOCK_TABLE_NAME);
            tableNameList.add(TRANSACTION_TABLE_NAME);
            paramInfo.setTableNameList(tableNameList);
//12-13.    // delete block + transaction from main db
            state = shardEngine.deleteCopiedData(paramInfo);
            assertEquals(MigrateState.DATA_REMOVED_FROM_MAIN, state);
//        assertEquals(MigrateState.FAILED, state);

            // checks after COPY + DELETE...
            count = blockchain.getBlockCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
            assertEquals(6, count); // total blocks left in main db
            count = blockchain.getTransactionCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
            assertEquals(10, count); // total transactions left in main db

            shardDataSource = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getBlockCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(8, count); // blocks in shard

            shardDataSource = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getTransactionCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(4, count); // transactions in shard

//14.       // complete shard process
            paramInfo.setShardHash(shardHash);
            state = shardEngine.addShardInfo(paramInfo);
            assertEquals(MigrateState.COMPLETED, state);

// compare fullhashes
            TransactionIndex index = transactionIndexDao.getByTransactionId(td.TRANSACTION_1.getId());
            assertNotNull(index);
            byte[] fullHash = Convert.toFullHash(index.getTransactionId(), index.getPartialTransactionHash());
            assertArrayEquals(td.TRANSACTION_1.getFullHash(), fullHash);
            log.debug("Migration finished in = {} sec", (System.currentTimeMillis() - start) / 1000);
        }
        finally {
//            AplCoreRuntime.getInstance().setup(null, null);
        }
    }
}