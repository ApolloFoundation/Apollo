/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.COMPLETED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.CSV_EXPORT_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_COPY_TO_SHARD_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_REMOVED_FROM_MAIN;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.MAIN_DB_BACKUPED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SECONDARY_INDEX_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.ZIP_ARCHIVE_FINISHED;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_12_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
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
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.commands.BackupDbBeforeShardCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CopyDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CreateShardSchemaCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CsvExportCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DeleteCopiedDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.FinishShardingCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.UpdateSecondaryIndexCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.ZipArchiveCommand;
import com.apollocurrency.aplwallet.apl.core.shard.hash.ShardHashCalculatorImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporterImpl;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.UserMode;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jboss.weld.literal.NamedLiteral;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import org.junit.jupiter.api.Disabled;

@EnableWeld
class ShardMigrationExecutorTest {

    private static final String SHA_512 = "SHA-512";

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(getTempFilePath("shardMigrationTestDb").toAbsolutePath().toString()));
    static PropertiesHolder propertiesHolder = initPropertyHolder();
    private static BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private static HeightConfig heightConfig = mock(HeightConfig.class);

    private final Bean<Path> dataExportDir = MockBean.of(temporaryFolderExtension.newFolder().toPath().toAbsolutePath(), Path.class);
    {
        dataExportDir.getQualifiers().add(new NamedLiteral("dataExportDir"));
    }
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ReferencedTransactionDao.class,
            PropertyProducer.class,
            GlobalSyncImpl.class, BlockIndexDao.class, ShardHashCalculatorImpl.class,
            DerivedDbTablesRegistryImpl.class, ShardEngineImpl.class, ShardRecoveryDao.class,
            ShardRecoveryDaoJdbcImpl.class, ShardDao.class, ShardRecoveryDao.class,
            ExcludedTransactionDbIdExtractor.class,
            FullTextConfigImpl.class,
            DerivedTablesRegistry.class,
            ShardEngineImpl.class, CsvExporterImpl.class, ShardDaoJdbcImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class, TrimService.class, ShardMigrationExecutor.class,
            AplCoreRuntime.class,
            AplAppStatus.class)
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(dataExportDir)
            .addBeans(MockBean.of(Mockito.mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .build();
    @Inject
    AplCoreRuntime aplCoreRuntime;
    @Inject
    private ShardEngine shardEngine;
    @Inject
    private ShardMigrationExecutor shardMigrationExecutor;
    @Inject
    private BlockIndexDao blockIndexDao;
    @Inject
    private Blockchain blockchain;
    @Inject
    private TransactionIndexDao transactionIndexDao;
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

    public ShardMigrationExecutorTest() throws Exception {}


    @BeforeAll
    static void setUpAll() {

        Mockito.doReturn(SHA_512).when(heightConfig).getShardingDigestAlgorithm();
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
    }

    @BeforeEach
    void setUp() {
        blockchain.setLastBlock(new BlockTestData().LAST_BLOCK);
    }

    @AfterEach
    void tearDown() {
        extension.getDatabaseManger().shutdown();
    }

    @Test
    void executeAllOperations() throws IOException {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("backup").toPath()).when(dirProvider).getDbDir();
        //TODO: YL, do we really need it all here?
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME, 1);
        aplCoreRuntime.setup(new UserMode(), dirProvider, configDirProvider );
        try {
            int snapshotBlockHeight = 8000;

            // prepare an save Recovery + new Shard info
            ShardRecovery recovery = new ShardRecovery(MigrateState.INIT);
            recoveryDao.saveShardRecovery(extension.getDatabaseManger().getDataSource(), recovery);
            Shard newShard = new Shard(snapshotBlockHeight);
            shardDao.saveShard(newShard);

            MigrateState state;

//1.        // create main db backup
            BackupDbBeforeShardCommand beforeShardCommand = new BackupDbBeforeShardCommand(shardEngine);
            state = shardMigrationExecutor.executeOperation(beforeShardCommand);
            assertEquals(MAIN_DB_BACKUPED, state);
            assertTrue(Files.exists(dirProvider.getDbDir().resolve("BACKUP-BEFORE-apl-blockchain-shard-4.zip")));

//2.        // create shard db with 'initial' schema
            CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(shardEngine,
                    new ShardInitTableSchemaVersion());
            state = shardMigrationExecutor.executeOperation(createShardSchemaCommand);
            assertEquals(SHARD_SCHEMA_CREATED, state);

            // checks before COPYING blocks / transactions
            long count = blockchain.getBlockCount(null, 0, BLOCK_12_HEIGHT + 1); // upper bound is excluded, so +1
            assertEquals(14, count); // total blocks in main db
            count = blockchain.getTransactionCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
            assertEquals(14, count); // total transactions in main db

            TransactionTestData td = new TransactionTestData();
            Set<Long> dbIds = new HashSet<>();
//            dbIds.add(td.DB_ID_6);
//            dbIds.add(td.DB_ID_10);
            dbIds.add(td.DB_ID_0);
            dbIds.add(td.DB_ID_2);
            dbIds.add(td.DB_ID_5);

//3-4.      // copy block + transaction data from main db into shard
            CopyDataCommand copyDataCommand = new CopyDataCommand(shardEngine, snapshotBlockHeight, dbIds);
            state = shardMigrationExecutor.executeOperation(copyDataCommand);
//        assertEquals(FAILED, state);
            assertEquals(DATA_COPY_TO_SHARD_FINISHED, state);

            // check after COPY
            TransactionalDataSource shardDataSource = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getBlockCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(8, count); // blocks in shard db
            shardDataSource = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getTransactionCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(4, count);// transactions in shard db

//5.        // create shard db FULL schema
            createShardSchemaCommand = new CreateShardSchemaCommand(shardEngine,
                    new ShardAddConstraintsSchemaVersion());
            state = shardMigrationExecutor.executeOperation(createShardSchemaCommand);
            assertEquals(SHARD_SCHEMA_FULL, state);

//            ReLinkDataCommand reLinkDataCommand = new ReLinkDataCommand(shardEngine, snapshotBlockHeight, dbIds);
//            state = shardMigrationExecutor.executeOperation(reLinkDataCommand);
//            assertEquals(DATA_RELINKED_IN_MAIN, state);

//6-7.      // update secondary block + transaction indexes
            UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand(shardEngine, snapshotBlockHeight, dbIds);
            state = shardMigrationExecutor.executeOperation(updateSecondaryIndexCommand);
//        assertEquals(FAILED, state);
            assertEquals(SECONDARY_INDEX_FINISHED, state);

            // check by secondary indexes
            long blockIndexCount = blockIndexDao.countBlockIndexByShard(4L);
            // should be 8 but prev shard already exist and grabbed our genesis block
            assertEquals(7, blockIndexCount);
            long trIndexCount = transactionIndexDao.countTransactionIndexByShardId(4L);
            assertEquals(4, trIndexCount);

            Transaction tx = blockchain.getTransaction(td.TRANSACTION_2.getId());
            assertEquals(td.TRANSACTION_2, tx); // check that transaction was ignored and left in main db

//8-9.      // export 'derived', shard, secondary block + transaction indexes
            CsvExportCommand csvExportCommand = new CsvExportCommand(shardEngine, snapshotBlockHeight, dbIds);
            state = shardMigrationExecutor.executeOperation(csvExportCommand);
//        assertEquals(FAILED, state);
            assertEquals(CSV_EXPORT_FINISHED, state);

//10-11.    // archive CSV into zip
            ZipArchiveCommand zipArchiveCommand = new ZipArchiveCommand(shardEngine);
            state = shardMigrationExecutor.executeOperation(zipArchiveCommand);
//        assertEquals(FAILED, state);
            assertEquals(ZIP_ARCHIVE_FINISHED, state);

//12-13.    // delete block + transaction from main db
            DeleteCopiedDataCommand deleteCopiedDataCommand = new DeleteCopiedDataCommand(shardEngine, snapshotBlockHeight, dbIds);
            state = shardMigrationExecutor.executeOperation(deleteCopiedDataCommand);
//        assertEquals(FAILED, state);
            assertEquals(DATA_REMOVED_FROM_MAIN, state);

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
            byte[] shardHash = "000000000".getBytes();
            FinishShardingCommand finishShardingCommand = new FinishShardingCommand(shardEngine, shardHash);
            state = shardMigrationExecutor.executeOperation(finishShardingCommand);
            assertEquals(COMPLETED, state);
        } finally {
//            AplCoreRuntime.getInstance().setup(null, null); //remove when AplCoreRuntime become an injectable bean
        }
    }

    @Test
    void executeAll() {
        shardMigrationExecutor.createAllCommands(8000);
        MigrateState state = shardMigrationExecutor.executeAllOperations();
//        assertEquals(FAILED, state);
        assertEquals(COMPLETED, state);
    }


    private Path getTempFilePath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to create shard db file", e);
        }
    }
    private static PropertiesHolder initPropertyHolder() {
        PropertiesHolder propertiesHolder = new PropertiesHolder();
        Properties properties = new Properties();
        properties.put("apl.trimFrequency", 1000);
        properties.put("apl.trimDerivedTables", true);
        properties.put("apl.maxRollback", 21600);

        propertiesHolder.init(properties);
        return propertiesHolder;

    }
}