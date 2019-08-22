/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeServiceImpl;
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
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
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
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.PrevBlockData;
import com.apollocurrency.aplwallet.apl.core.shard.model.TableInfo;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;
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

import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.COMPLETED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.CSV_EXPORT_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_COPY_TO_SHARD_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_REMOVED_FROM_MAIN;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.MAIN_DB_BACKUPED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SECONDARY_INDEX_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.ZIP_ARCHIVE_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.GOODS_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.PHASING_POLL_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.SHARD_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.TRANSACTION_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.TRANSACTION_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_12_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EnableWeld
class ShardMigrationExecutorTest {

    private static final String SHA_512 = "SHA-512";

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("targetDb").toAbsolutePath().toString()));
    static PropertiesHolder propertiesHolder = initPropertyHolder();
    private static BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private static HeightConfig heightConfig = mock(HeightConfig.class);

    private final Path dataExportDirPath = createPath("targetDb");
    private final Bean<Path> dataExportDir = MockBean.of(dataExportDirPath.toAbsolutePath(), Path.class);
    private DirProvider dirProvider = mock(DirProvider.class);
    {
        // return the same dir for both CDI components
        dataExportDir.getQualifiers().add(new NamedLiteral("dataExportDir")); // for CsvExporter
        doReturn(dataExportDirPath).when(dirProvider).getDataExportDir(); // for Zip
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
            DGSGoodsTable.class,
            PrevBlockInfoExtractor.class,
            PhasingPollTable.class,
            FullTextConfigImpl.class,
            DerivedTablesRegistry.class,
            ShardEngineImpl.class, CsvExporterImpl.class, ZipImpl.class, AplAppStatus.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class, ShardMigrationExecutor.class,
            AplAppStatus.class)
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(PeersService.class), PeersService.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .addBeans(MockBean.of(dirProvider, DirProvider.class))
            .addBeans(MockBean.of(mock(TrimService.class), TrimService.class))
            .addBeans(dataExportDir)
            .addBeans(MockBean.of(Mockito.mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
            .build();
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
    @Inject
    private DGSGoodsTable goodsTable;
    @Inject
    private PhasingPollTable phasingPollTable;

    public ShardMigrationExecutorTest() throws Exception {}

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

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
        extension.getDatabaseManager().shutdown();
    }

    @Test
    void executeAllOperations() throws IOException {
        doReturn(temporaryFolderExtension.newFolder("backup").toPath()).when(dirProvider).getDbDir();

        int snapshotBlockHeight = 8000;

        // prepare an save Recovery + new Shard info
        ShardRecovery recovery = new ShardRecovery(MigrateState.INIT);
        recoveryDao.saveShardRecovery(extension.getDatabaseManager().getDataSource(), recovery);
        long shardId = shardDao.getNextShardId();
        long[] dbIdsExclude = new long[]{BlockTestData.BLOCK_9_GENERATOR, BlockTestData.BLOCK_8_GENERATOR, BlockTestData.BLOCK_7_GENERATOR};
        Shard newShard = new Shard(shardId, snapshotBlockHeight);
        newShard.setGeneratorIds(dbIdsExclude);
        shardDao.saveShard(newShard);

            MigrateState state;

//1.        // create main db backup
            BackupDbBeforeShardCommand beforeShardCommand = new BackupDbBeforeShardCommand(shardEngine);
            state = shardMigrationExecutor.executeOperation(beforeShardCommand);
            assertEquals(MAIN_DB_BACKUPED, state);
            assertTrue(Files.exists(dirProvider.getDbDir().resolve("BACKUP-BEFORE-apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip")));

//2.        // create shard db with 'initial' schema
            CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(4L, shardEngine,
                    new ShardInitTableSchemaVersion(), null, null);
            state = shardMigrationExecutor.executeOperation(createShardSchemaCommand);
            assertEquals(SHARD_SCHEMA_CREATED, state);

            // checks before COPYING blocks / transactions
            long count = blockchain.getBlockCount(null, 0, BLOCK_12_HEIGHT + 1); // upper bound is excluded, so +1
            assertEquals(14, count); // total blocks in main db
            count = blockchain.getTransactionCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
            assertEquals(14, count); // total transactions in main db

            TransactionTestData td = new TransactionTestData();
        ExcludeInfo excludeInfo = new ExcludeInfo(
                List.of(new TransactionDbInfo(td.DB_ID_0, td.TRANSACTION_0.getId())),
                List.of(new TransactionDbInfo(td.DB_ID_2, td.TRANSACTION_2.getId())),
                List.of(new TransactionDbInfo(td.DB_ID_5, td.TRANSACTION_5.getId()))
        );

//3-4.      // copy block + transaction data from main db into shard
            CopyDataCommand copyDataCommand = new CopyDataCommand(4L, shardEngine, snapshotBlockHeight, excludeInfo);
            state = shardMigrationExecutor.executeOperation(copyDataCommand);
            assertEquals(DATA_COPY_TO_SHARD_FINISHED, state);

            // check after COPY
            TransactionalDataSource shardDataSource = ((ShardManagement) extension.getDatabaseManager()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getBlockCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(8, count); // blocks in shard db
            shardDataSource = ((ShardManagement) extension.getDatabaseManager()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getTransactionCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(5, count);// transactions in shard db

//5.        // create shard db FULL schema
            byte[] shardHash = "0123456780".getBytes(); // just an example
            createShardSchemaCommand = new CreateShardSchemaCommand(4L, shardEngine,
                    new ShardAddConstraintsSchemaVersion(), shardHash, PrevBlockData.builder().generatorIds(new Long[]{1L, 2L}).prevBlockTimeouts(new Integer[] {3, 4}).prevBlockTimestamps(new Integer[] {5, 6}).build());
            state = shardMigrationExecutor.executeOperation(createShardSchemaCommand);
            assertEquals(SHARD_SCHEMA_FULL, state);

            Shard shard = shardDao.getShardById(shardId);
            assertNotNull(shard);
            assertArrayEquals(shardHash, shard.getShardHash());
            assertArrayEquals(new long[] {1, 2}, shard.getGeneratorIds());
            assertArrayEquals(new int[] {3, 4}, shard.getBlockTimeouts());
            assertArrayEquals(new int[] {5, 6}, shard.getBlockTimestamps());


//6-7.      // update secondary block + transaction indexes
            UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand(shardEngine, snapshotBlockHeight, excludeInfo);
            state = shardMigrationExecutor.executeOperation(updateSecondaryIndexCommand);
            assertEquals(SECONDARY_INDEX_FINISHED, state);

            // check by secondary indexes
            long blockIndexCount = blockIndexDao.countBlockIndexByShard(4L);
            // should be 8 but prev shard already exist and grabbed our genesis block
            assertEquals(7, blockIndexCount);
            long trIndexCount = transactionIndexDao.countTransactionIndexByShardId(4L);
            assertEquals(5, trIndexCount);

            Transaction tx = blockchain.getTransaction(td.TRANSACTION_2.getId());
            assertEquals(td.TRANSACTION_2, tx); // check that transaction was ignored and left in main db

//8-9.      // export 'derived', shard, secondary block + transaction indexes
        List<TableInfo> tables = List.of(BLOCK_TABLE_NAME, TRANSACTION_TABLE_NAME, TRANSACTION_INDEX_TABLE_NAME, BLOCK_INDEX_TABLE_NAME, SHARD_TABLE_NAME, GOODS_TABLE_NAME, PHASING_POLL_TABLE_NAME).stream().map(TableInfo::new).collect(Collectors.toList());

        CsvExportCommand csvExportCommand = new CsvExportCommand(shardEngine, 1, snapshotBlockHeight, tables, excludeInfo);
            state = shardMigrationExecutor.executeOperation(csvExportCommand);
            assertEquals(CSV_EXPORT_FINISHED, state);

//10-11.    // archive CSV into zip
            ZipArchiveCommand zipArchiveCommand = new ZipArchiveCommand(4L, tables, shardEngine);
            state = shardMigrationExecutor.executeOperation(zipArchiveCommand);
            assertEquals(ZIP_ARCHIVE_FINISHED, state);

//12-13.    // delete block + transaction from main db
            DeleteCopiedDataCommand deleteCopiedDataCommand = new DeleteCopiedDataCommand(shardEngine, snapshotBlockHeight, excludeInfo);
            state = shardMigrationExecutor.executeOperation(deleteCopiedDataCommand);
            assertEquals(DATA_REMOVED_FROM_MAIN, state);

            // checks after COPY + DELETE...
            count = blockchain.getBlockCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
            assertEquals(6, count); // total blocks left in main db
            count = blockchain.getTransactionCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
            assertEquals(9, count); // total transactions left in main db

            shardDataSource = ((ShardManagement) extension.getDatabaseManager()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getBlockCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(8, count); // blocks in shard

            shardDataSource = ((ShardManagement) extension.getDatabaseManager()).getOrCreateShardDataSourceById(4L);
            count = blockchain.getTransactionCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
            assertEquals(5, count); // transactions in shard

//14.       // complete shard process
            FinishShardingCommand finishShardingCommand = new FinishShardingCommand(shardEngine, 4L);
            state = shardMigrationExecutor.executeOperation(finishShardingCommand);
        assertEquals(COMPLETED, state);
    }

    @Test
    void executeFromRemovedData() {
        shardDao.saveShard(new Shard(4L, 8000));
        executeFrom(8000, 4L, DATA_REMOVED_FROM_MAIN);
        Shard lastShard = shardDao.getLastShard();
        assertNotNull(lastShard);
        assertEquals(ShardState.FULL, lastShard.getShardState());
    }

    private void executeFrom(int height, long shardId, MigrateState state) {
        shardMigrationExecutor.createAllCommands(height, shardId, state);
        MigrateState result = shardMigrationExecutor.executeAllOperations();
        assertEquals(COMPLETED, result);
    }

    @Test
    void executeAll() {
        executeFrom(8000, 4L, MigrateState.INIT);
    }

    private static PropertiesHolder initPropertyHolder() {
        PropertiesHolder propertiesHolder = new PropertiesHolder();
        Properties properties = new Properties();
        properties.put("apl.trimDerivedTables", true);
        properties.put("apl.maxRollback", 21600);

        propertiesHolder.init(properties);
        return propertiesHolder;

    }
}