/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.MAIN_DB_BACKUPED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.SHARD_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.TRANSACTION_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.TRANSACTION_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase.CSV_FILE_EXTENSION;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_12_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.dao.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TrimDao;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalInterceptor;
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
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporterImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaperImpl;
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.PrevBlockData;
import com.apollocurrency.aplwallet.apl.core.shard.model.TableInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.environment.se.Weld;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;

@EnableWeld
class ShardEngineTest {
    private static final Logger log = getLogger(ShardEngineTest.class);
    static final String GOODS_TABLE_NAME = "goods";
    static final String PHASING_POLL_TABLE_NAME = "phasing_poll";
    static final String PRUNABLE_MESSAGE_TABLE_NAME = "prunable_message";

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

    private final Path dataExportDirPath = createPath("targetDb");
    private final Bean<Path> dataExportDir = MockBean.of(dataExportDirPath.toAbsolutePath(), Path.class);
    private DirProvider dirProvider = mock(DirProvider.class);
    private Zip zip = spy(new ZipImpl());
    private CsvEscaper translator = new CsvEscaperImpl();

    private CsvExporter csvExporter = spy(new CsvExporterImpl(extension.getDatabaseManager(), dataExportDirPath, translator));
    Weld weld = WeldInitiator.createWeld();
    {
        weld.addInterceptor(JdbiTransactionalInterceptor.class);
        weld.addBeanClasses(PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class, ReferencedTransactionDao.class, ShardDao.class, ShardRecoveryDao.class,
                DerivedDbTablesRegistryImpl.class, JdbiTransactionalInterceptor.class,
                TransactionTestData.class, PropertyProducer.class, ShardRecoveryDaoJdbcImpl.class,
                GlobalSyncImpl.class, FullTextConfigImpl.class, FullTextConfig.class,
                DGSGoodsTable.class, PrunableMessageServiceImpl.class, PrunableMessageTable.class,
                PhasingPollTable.class,
                DerivedTablesRegistry.class,
                ShardEngineImpl.class, AplAppStatus.class, BlockDaoImpl.class, TransactionDaoImpl.class, TrimService.class, TrimDao.class
                );

        // return the same dir for both CDI components
        dataExportDir.getQualifiers().add(new NamedLiteral("dataExportDir")); // for CsvExporter
        doReturn(dataExportDirPath).when(dirProvider).getDataExportDir(); // for Zip
    }

    @WeldSetup
    public WeldInitiator weldInitiator = WeldInitiator.from(weld)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(mock(ConfigDirProvider.class), ConfigDirProvider.class))
            .addBeans(MockBean.of(dirProvider, DirProvider.class))
            .addBeans(MockBean.of(csvExporter, CsvExporter.class))
            .addBeans(MockBean.of(zip, Zip.class))
            .addBeans(dataExportDir)
            .addBeans(MockBean.of(mock(TimeService.class), TimeService.class))
            .addBeans(MockBean.of(mock(AccountPublicKeyService.class), AccountPublicKeyService.class, AccountPublicKeyServiceImpl.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
            .addBeans(MockBean.of(translator, CsvEscaperImpl.class))
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

    @Inject
    DGSGoodsTable goodsTable;
    @Inject
    TransactionDao transactionDao;
    @Inject
    PhasingPollTable phasingPollTable;
    @Inject
    PrunableMessageTable messageTable;


    public ShardEngineTest() throws Exception {}

    private Path createPath(String fileName) {
        try {
            Path path = temporaryFolderExtension.newFolder().toPath().resolve(fileName);
            Files.createDirectories(path);
            return path;
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
        state = shardEngine.addOrCreateShard(new ShardInitTableSchemaVersion(), CommandParamInfo.builder().shardId(3L).build());
        assertEquals(SHARD_SCHEMA_CREATED, state);

        checkDbVersion(5, 3);
        checkTableExist(new String[] {"block", "option", "transaction"}, 3);
    }

    private void checkTableExist(String[] strings, int shardId) {
        TransactionalDataSource dataSource = ((ShardManagement) extension.getDatabaseManager()).getShardDataSourceById(shardId);
        assertNotNull(dataSource, "Shard datasource should be initialized");
        for (String table : strings) {
            DbUtils.inTransaction(dataSource, (con) -> {
                try  {
                    con.createStatement().executeQuery("select 1 from " + table);
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            });
        }
    }

    private void checkDbVersion(int version, int shardId) {
        TransactionalDataSource dataSource = ((ShardManagement) extension.getDatabaseManager()).getShardDataSourceById(shardId);
        assertNotNull(dataSource, "Shard datasource should be initialized");
        DbUtils.inTransaction(dataSource, (con)-> {
            try(PreparedStatement pstmt = con.prepareStatement("select * from version");
                ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next(), "Version table should contain rows");
                assertEquals(version, rs.getInt(1));
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        });
    }

    @Test
    void createFullShardDb() throws IOException {
        MigrateState state = shardEngine.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        byte[] shardHash = new byte[32];
        Long[] generators = {1L, 2L, 3L};
        Integer[] timestamps = {4, 5, 6};
        Integer[] timeouts = {7, 8, 9};
        PrevBlockData prevBlockData = PrevBlockData.builder()
                .prevBlockTimestamps(timestamps)
                .prevBlockTimeouts(timeouts)
                .generatorIds(generators)
                .build();
        state = shardEngine.addOrCreateShard(new ShardAddConstraintsSchemaVersion(), CommandParamInfo.builder().shardHash(shardHash).shardId(3L).prevBlockData(prevBlockData).build());
        assertEquals(SHARD_SCHEMA_FULL, state);
        checkDbVersion(24, 3);
        checkTableExist(new String[] {"block", "option", "transaction"}, 3);
        Shard lastShard = shardDao.getLastShard();
        assertArrayEquals(generators, Convert.toArray(lastShard.getGeneratorIds()));
        assertArrayEquals(timeouts, Convert.toArray(lastShard.getBlockTimeouts()));
        assertArrayEquals(timestamps, Convert.toArray(lastShard.getBlockTimestamps()));
        assertArrayEquals(shardHash, lastShard.getShardHash());
    }

    @Test
    void createSchemaShardDbWhenAlreadyCreatedByRecovery() {
        createShardDbWhenAlreadyCreated(new ShardInitTableSchemaVersion(), SHARD_SCHEMA_CREATED);
    }

    @Test
    void createFullShardDbWhenAlreadyCreatedByRecovery() {
        createShardDbWhenAlreadyCreated(new ShardAddConstraintsSchemaVersion(), SHARD_SCHEMA_FULL);
    }

    private void createShardDbWhenAlreadyCreated(DbVersion dbVersion, MigrateState state) {
        DbUtils.inTransaction(extension, (con) -> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        ShardRecovery recovery = new ShardRecovery(state);
        TransactionalDataSource dataSource = extension.getDatabaseManager().getDataSource();
        shardRecoveryDaoJdbc.saveShardRecovery(dataSource, recovery);
        MigrateState shardState = shardEngine.addOrCreateShard(dbVersion, CommandParamInfo.builder().shardHash(new byte[32]).build());
        assertEquals(shardState, state);
        ShardRecovery actualRecovery = shardRecoveryDaoJdbc.getLatestShardRecovery(dataSource);
        assertEquals(state, actualRecovery.getState());
    }

    @Test
    void createFullShardDbWhenNoRecoveryPresent() {
        DbUtils.inTransaction(extension, (con) -> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        MigrateState shardState = shardEngine.addOrCreateShard(new ShardAddConstraintsSchemaVersion(), CommandParamInfo.builder().shardHash(new byte[32]).build());
        assertEquals(MigrateState.FAILED, shardState);
    }


    @Test
    void createShardDbDoAllOperations() throws IOException, SQLException {
        // folder to backup step
        doReturn(temporaryFolderExtension.newFolder("backup").toPath()).when(dirProvider).getDbDir();
        blockIndexDao.hardDeleteAllBlockIndex();

        long start = System.currentTimeMillis();
        MigrateState state = shardEngine.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        int snapshotBlockHeight = 8000;

        // prepare and save Recovery + new Shard info
        ShardRecovery recovery = new ShardRecovery(state);
        recoveryDao.saveShardRecovery(extension.getDatabaseManager().getDataSource(), recovery);
        byte[] shardHash = "0123456780".getBytes();
        long shardId = shardDao.getNextShardId();
        long[] dbIdsExclude = new long[]{BlockTestData.BLOCK_9_GENERATOR, BlockTestData.BLOCK_8_GENERATOR, BlockTestData.BLOCK_7_GENERATOR};
        Shard newShard = new Shard(shardHash, snapshotBlockHeight);
        newShard.setShardId(shardId);
        newShard.setGeneratorIds(dbIdsExclude);
        shardDao.saveShard(newShard);

//1.        // create main db backup
        state = shardEngine.createBackup();
        assertEquals(MAIN_DB_BACKUPED, state);
        assertTrue(Files.exists(dirProvider.getDbDir().resolve("BACKUP-BEFORE-apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip")));

//2.        // create shard db with 'initial' schema
        state = shardEngine.addOrCreateShard(new ShardInitTableSchemaVersion(), CommandParamInfo.builder().shardId(4L).build());
        assertEquals(SHARD_SCHEMA_CREATED, state);

        // checks before COPYING blocks / transactions
        long count = blockchain.getBlockCount(null, 0, BLOCK_12_HEIGHT + 1); // upper bound is excluded, so +1
        assertEquals(14, count); // total blocks in main db
        count = blockchain.getTransactionCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
        assertEquals(14, count); // total transactions in main db

        List<TableInfo> tableNameList = new ArrayList<>();
        tableNameList.add(new TableInfo(BLOCK_TABLE_NAME));
        tableNameList.add(new TableInfo(TRANSACTION_TABLE_NAME));
        TransactionTestData td = new TransactionTestData();
        ExcludeInfo excludeInfo = new ExcludeInfo(
                List.of(new TransactionDbInfo(td.DB_ID_0, td.TRANSACTION_0.getId())),
                List.of(new TransactionDbInfo(td.DB_ID_2, td.TRANSACTION_2.getId())),
                List.of(new TransactionDbInfo(td.DB_ID_5, td.TRANSACTION_5.getId()))
        );

        CommandParamInfo paramInfo = CommandParamInfo.builder()
                .shardId(4L)
                .tableInfoList(tableNameList)
                .commitBatchSize(2)
                .snapshotBlockHeight(snapshotBlockHeight)
                .excludeInfo(excludeInfo)
                .build();

//3-4.      // copy block + transaction data from main db into shard
        state = shardEngine.copyDataToShard(paramInfo);
        assertEquals(MigrateState.DATA_COPY_TO_SHARD_FINISHED, state);

        // check after COPY
        TransactionalDataSource shardDataSource = ((ShardManagement) extension.getDatabaseManager()).getOrCreateShardDataSourceById(4L);
        count = blockchain.getBlockCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1

        assertEquals(8, count); // blocks in shard db
        shardDataSource = ((ShardManagement) extension.getDatabaseManager()).getOrCreateShardDataSourceById(4L);
        count = blockchain.getTransactionCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
        assertEquals(5, count);// transactions in shard db
        Transaction excludedTransaction = transactionDao.findTransaction(td.TRANSACTION_0.getId(), shardDataSource); // excluded transaction #1
        assertNull(excludedTransaction);
        excludedTransaction = transactionDao.findTransaction(td.TRANSACTION_2.getId(), shardDataSource); // excluded transaction #2
        assertNull(excludedTransaction);
        assertEquals(td.TRANSACTION_5, transactionDao.findTransaction(td.TRANSACTION_5.getId(), shardDataSource));

        //5.        // create shard db FULL schema + add shard hash info
        state = shardEngine.addOrCreateShard(new ShardAddConstraintsSchemaVersion(), CommandParamInfo.builder().shardHash(shardHash).shardId(4L).prevBlockData( PrevBlockData.builder().generatorIds(new Long[]{1L, 2L}).prevBlockTimeouts(new Integer[] {3, 4}).prevBlockTimestamps(new Integer[] {5, 6}).build()).build());
        assertEquals(SHARD_SCHEMA_FULL, state);
        // check 'merkle tree hash' is stored in shard record
        Shard shard = shardDao.getShardById(shardId);
        assertNotNull(shard);
        assertArrayEquals(shardHash, shard.getShardHash());
        assertArrayEquals(new long[] {1, 2}, shard.getGeneratorIds());
        assertArrayEquals(new int[] {3, 4}, shard.getBlockTimeouts());
        assertArrayEquals(new int[] {5, 6}, shard.getBlockTimestamps());


        tableNameList.clear();
        tableNameList.add(new TableInfo(BLOCK_INDEX_TABLE_NAME));
        tableNameList.add(new TableInfo(TRANSACTION_INDEX_TABLE_NAME));
        paramInfo = CommandParamInfo.builder().snapshotBlockHeight(snapshotBlockHeight).commitBatchSize(2).excludeInfo(excludeInfo).shardId(4L).tableInfoList(tableNameList).build();

//6-7.      // update secondary block + transaction indexes
        state = shardEngine.updateSecondaryIndex(paramInfo);
        assertEquals(MigrateState.SECONDARY_INDEX_FINISHED, state);

        long blockIndexCount = blockIndexDao.countBlockIndexByShard(4L);
        // should be 8 but prev shard already exist and grabbed our genesis block
        assertEquals(7, blockIndexCount);
        long trIndexCount = transactionIndexDao.countTransactionIndexByShardId(4L);
        assertEquals(5, trIndexCount);

        assertNull(transactionIndexDao.getByTransactionId(td.TRANSACTION_0.getId())); // excluded txs should not be indexed
        assertNull(transactionIndexDao.getByTransactionId(td.TRANSACTION_2.getId())); // excluded txs should not be indexed
        assertNotNull(transactionIndexDao.getByTransactionId(td.TRANSACTION_5.getId()));


        tableNameList.clear();
        tableNameList.add(new TableInfo(SHARD_TABLE_NAME));
        tableNameList.add(new TableInfo(BLOCK_INDEX_TABLE_NAME));
        tableNameList.add(new TableInfo(TRANSACTION_INDEX_TABLE_NAME));
        tableNameList.add(new TableInfo(TRANSACTION_TABLE_NAME));
        tableNameList.add(new TableInfo(BLOCK_TABLE_NAME));
        tableNameList.add(new TableInfo(GOODS_TABLE_NAME));
        tableNameList.add(new TableInfo(PHASING_POLL_TABLE_NAME));
        tableNameList.add(new TableInfo(PRUNABLE_MESSAGE_TABLE_NAME, true));
        BlockTestData btd = new BlockTestData();
        Block block = mock(Block.class);

        doReturn(553327).when(block).getHeight();
        doReturn(btd.LAST_BLOCK.getTimestamp() + 10).when(block).getTimestamp();

        blockchain.setLastBlock(block);
        paramInfo = CommandParamInfo.builder().commitBatchSize(2).snapshotBlockHeight(553326).excludeInfo(excludeInfo).shardId(4L).tableInfoList(tableNameList).build();
//8-9.      // export 'derived', shard, secondary block + transaction indexes
        state = shardEngine.exportCsv(paramInfo);

        assertEquals(MigrateState.CSV_EXPORT_FINISHED, state);
        assertFalse(Files.exists(dataExportDirPath.resolve(tableToCsvFile(PHASING_POLL_TABLE_NAME ))));
        assertEquals(5, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(SHARD_TABLE_NAME )))                  .size());
        assertEquals(10,Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(TRANSACTION_INDEX_TABLE_NAME )))      .size());
        assertEquals(9, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(BLOCK_INDEX_TABLE_NAME )))            .size());
        assertEquals(9, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(GOODS_TABLE_NAME )))                  .size());
        assertEquals(4, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(TRANSACTION_TABLE_NAME )))            .size());
        assertEquals(2, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(BLOCK_TABLE_NAME )))                  .size());


        paramInfo = CommandParamInfo.builder().snapshotBlockHeight(snapshotBlockHeight).excludeInfo(excludeInfo).shardId(4L).tableInfoList(tableNameList).build();
//10-11.    // archive CSV into zip
        state = shardEngine.archiveCsv(paramInfo);
        assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, state);
        assertTrue(Files.exists(dirProvider.getDataExportDir().resolve("apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip")));
        assertTrue(Files.exists(dirProvider.getDataExportDir().resolve("apl-blockchain-shardprun-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip")));

        tableNameList.clear();
        tableNameList.add(new TableInfo(BLOCK_TABLE_NAME));
        tableNameList.add(new TableInfo(TRANSACTION_TABLE_NAME));
        paramInfo = CommandParamInfo.builder().snapshotBlockHeight(snapshotBlockHeight).commitBatchSize(2).excludeInfo(excludeInfo).shardId(4L).tableInfoList(tableNameList).build();

//12-13.    // delete block + transaction from main db
        state = shardEngine.deleteCopiedData(paramInfo);
        assertEquals(MigrateState.DATA_REMOVED_FROM_MAIN, state);

        // checks after COPY + DELETE...
        count = blockchain.getBlockCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
        assertEquals(6, count); // total blocks left in main db
        count = blockchain.getTransactionCount(null, 0, BLOCK_12_HEIGHT + 1);// upper bound is excluded, so +1
        assertEquals(9, count); // total transactions left in main db
        assertNull(blockchain.getTransaction(td.TRANSACTION_0.getId())); //deleted finished phased transaction

        shardDataSource = ((ShardManagement) extension.getDatabaseManager()).getOrCreateShardDataSourceById(4L);
        count = blockchain.getBlockCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
        assertEquals(8, count); // blocks in shard

        shardDataSource = ((ShardManagement) extension.getDatabaseManager()).getOrCreateShardDataSourceById(4L);
        count = blockchain.getTransactionCount(shardDataSource, 0, snapshotBlockHeight + 1);// upper bound is excluded, so +1
        assertEquals(5, count); // transactions in shard

//14.       // complete shard process
        paramInfo = CommandParamInfo.builder().shardId(4L).build();
        state = shardEngine.finishShardProcess(paramInfo);
        assertEquals(MigrateState.COMPLETED, state);
        Shard lastShard = shardDao.getLastShard();
        assertNotNull(lastShard.getPrunableZipHash());
        assertNotNull(lastShard.getCoreZipHash());
        List<ShardRecovery> existingRecovery = shardRecoveryDaoJdbc.getAllShardRecovery(extension.getDatabaseManager().getDataSource().getConnection());
        assertEquals(0, existingRecovery.size());
        // compare full hashes
        TransactionIndex index = transactionIndexDao.getByTransactionId(td.TRANSACTION_1.getId());
        assertNotNull(index);
        byte[] fullHash = Convert.toFullHash(index.getTransactionId(), index.getPartialTransactionHash());
        assertArrayEquals(td.TRANSACTION_1.getFullHash(), fullHash);
        log.debug("Migration finished in = {} sec", (System.currentTimeMillis() - start) / 1000);
    }

    @Test
    void testExportCsvWithExceptionRecovery() throws IOException {
        BlockTestData btd = new BlockTestData();
        TransactionTestData ttd = new TransactionTestData();
        blockchain.setLastBlock(btd.BLOCK_11);
        int snaphotBlockHeight = btd.BLOCK_10.getHeight() - 1;
        int batchLimit = 1;
        List<TableInfo> tables = List.of(SHARD_TABLE_NAME, TRANSACTION_INDEX_TABLE_NAME, TRANSACTION_TABLE_NAME, BLOCK_TABLE_NAME, ShardConstants.GOODS_TABLE_NAME, BLOCK_INDEX_TABLE_NAME, ShardConstants.PHASING_POLL_TABLE_NAME).stream().map(TableInfo::new).collect(Collectors.toList());
        ExcludeInfo excludeInfo = new ExcludeInfo(
                List.of(),
                List.of(new TransactionDbInfo(ttd.DB_ID_3, ttd.TRANSACTION_3.getId())),
                List.of(new TransactionDbInfo(ttd.DB_ID_5, ttd.TRANSACTION_5.getId()))
        );
        CommandParamInfo paramInfo = CommandParamInfo.builder().tableInfoList(tables).commitBatchSize(batchLimit).snapshotBlockHeight(snaphotBlockHeight).excludeInfo(excludeInfo).build();
        doThrow(IllegalStateException.class).when(csvExporter).exportBlock(snaphotBlockHeight);

        MigrateState state = shardEngine.exportCsv(paramInfo);

        assertEquals(MigrateState.FAILED, state);

        assertEquals(4, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(SHARD_TABLE_NAME))).size());
        assertEquals(5, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(TRANSACTION_INDEX_TABLE_NAME))).size());
        assertEquals(3, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(TRANSACTION_TABLE_NAME))).size());
        assertFalse(Files.exists(dataExportDirPath.resolve(tableToCsvFile(BLOCK_TABLE_NAME))));
        assertFalse(Files.exists(dataExportDirPath.resolve(tableToCsvFile(BLOCK_INDEX_TABLE_NAME))));
        verify(csvExporter, never()).exportBlockIndex(snaphotBlockHeight, batchLimit);
        verify(csvExporter, never()).exportDerivedTable(goodsTable, snaphotBlockHeight, batchLimit);
        verify(csvExporter, never()).exportDerivedTable(phasingPollTable, snaphotBlockHeight, batchLimit);
        ShardRecovery latestShardRecovery = shardRecoveryDaoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
        assertEquals(String.join(",",SHARD_TABLE_NAME, TRANSACTION_INDEX_TABLE_NAME, TRANSACTION_TABLE_NAME), latestShardRecovery.getProcessedObject());
    }

    @Test
    void testExportWithExistingRecovery() throws IOException {
        BlockTestData btd = new BlockTestData();
        TransactionTestData ttd = new TransactionTestData();
        blockchain.setLastBlock(btd.BLOCK_12);
        int snaphotBlockHeight = btd.BLOCK_10.getHeight() - 1;
        int batchLimit = 1;
        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        shardRecoveryDaoJdbc.saveShardRecovery(extension.getDatabaseManager().getDataSource(), new ShardRecovery(MigrateState.CSV_EXPORT_STARTED, null, null, null, "block,transaction_shard_index,shard"));
        List<TableInfo> tables = List.of(SHARD_TABLE_NAME, TRANSACTION_INDEX_TABLE_NAME, TRANSACTION_TABLE_NAME, BLOCK_TABLE_NAME, ShardConstants.GOODS_TABLE_NAME, BLOCK_INDEX_TABLE_NAME, ShardConstants.PHASING_POLL_TABLE_NAME).stream().map(TableInfo::new).collect(Collectors.toList());

        ExcludeInfo excludeInfo = new ExcludeInfo(
                List.of(),
                List.of(new TransactionDbInfo(ttd.DB_ID_3, ttd.TRANSACTION_3.getId())),
                List.of(new TransactionDbInfo(ttd.DB_ID_5, ttd.TRANSACTION_5.getId()))
        );
        CommandParamInfo paramInfo = CommandParamInfo.builder().tableInfoList(tables).commitBatchSize(batchLimit).snapshotBlockHeight(snaphotBlockHeight).excludeInfo(excludeInfo).build();
        Path transactionPath = dataExportDirPath.resolve("transaction.csv");
        Files.createFile(transactionPath);
        Files.write(transactionPath, List.of("Str-0", "Str-1", "Str-2", "Str-3", "Str-4", "Str-5", "Str-6"));

        MigrateState state = shardEngine.exportCsv(paramInfo);

        assertEquals(MigrateState.CSV_EXPORT_FINISHED, state);
        assertFalse(Files.exists(dataExportDirPath.resolve(tableToCsvFile(SHARD_TABLE_NAME)))                  );
        assertFalse(Files.exists(dataExportDirPath.resolve(tableToCsvFile(TRANSACTION_INDEX_TABLE_NAME))));
        assertFalse(Files.exists(dataExportDirPath.resolve(tableToCsvFile(BLOCK_TABLE_NAME)))                  );
        assertFalse(Files.exists(dataExportDirPath.resolve(tableToCsvFile(GOODS_TABLE_NAME))));

        assertEquals(3, Files.readAllLines(transactionPath)             .size());
        assertEquals(4, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(BLOCK_INDEX_TABLE_NAME)))            .size());
//        assertEquals(3, Files.readAllLines(dataExportDirPath.resolve(tableToCsvFile(PHASING_POLL_TABLE_NAME)))            .size());
        verify(csvExporter, never()).exportBlock(snaphotBlockHeight);
        verify(csvExporter, never()).exportShardTable(snaphotBlockHeight, batchLimit);
        verify(csvExporter, never()).exportTransactionIndex(snaphotBlockHeight, batchLimit);
        ShardRecovery latestShardRecovery = shardRecoveryDaoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
        assertEquals(MigrateState.CSV_EXPORT_FINISHED, latestShardRecovery.getState());
    }

    private String tableToCsvFile(String table) {
        return table + CSV_FILE_EXTENSION;
    }

    @Test
    void testExportAlreadyExported() {
        BlockTestData btd = new BlockTestData();
        int snaphotBlockHeight = btd.BLOCK_10.getHeight();
        int batchLimit = 1;
        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        shardRecoveryDaoJdbc.saveShardRecovery(extension.getDatabaseManager().getDataSource(), new ShardRecovery(MigrateState.ZIP_ARCHIVE_STARTED, null, null, null, "block,transaction_shard_index,shard"));
        List<TableInfo> tables = List.of(SHARD_TABLE_NAME, TRANSACTION_INDEX_TABLE_NAME, TRANSACTION_TABLE_NAME, BLOCK_TABLE_NAME, ShardConstants.GOODS_TABLE_NAME, BLOCK_INDEX_TABLE_NAME, ShardConstants.PHASING_POLL_TABLE_NAME).stream().map(TableInfo::new).collect(Collectors.toList());
        CommandParamInfo paramInfo = CommandParamInfo.builder().tableInfoList(tables).commitBatchSize(batchLimit).snapshotBlockHeight(snaphotBlockHeight).build();

        MigrateState state = shardEngine.exportCsv(paramInfo);

        assertEquals(MigrateState.CSV_EXPORT_FINISHED, state);
        verifyZeroInteractions(csvExporter);
    }

    @Test
    void testExportTablesWhichAreNotPresentInDerivedTablesRegistry() {
        BlockTestData btd = new BlockTestData();
        blockchain.setLastBlock(btd.BLOCK_12);
        int snaphotBlockHeight = btd.BLOCK_10.getHeight() - 1;
        int batchLimit = 1;
        List<TableInfo> tables = List.of(new TableInfo("invalid_table"));
        CommandParamInfo paramInfo = CommandParamInfo.builder().tableInfoList(tables).commitBatchSize(batchLimit).snapshotBlockHeight(snaphotBlockHeight).build();

        MigrateState state = shardEngine.exportCsv(paramInfo);

        assertEquals(MigrateState.FAILED, state);
        verify(csvExporter, times(2)).getDataExportPath();
        verifyNoMoreInteractions(csvExporter);
    }

    @Test
    void testExportTablesWhenRecoveryInfoNotExist() {
        BlockTestData btd = new BlockTestData();
        blockchain.setLastBlock(btd.BLOCK_12);
        int snaphotBlockHeight = btd.BLOCK_10.getHeight() - 1;
        int batchLimit = 1;
        List<TableInfo> tables = List.of(new TableInfo(GOODS_TABLE_NAME));
        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        CommandParamInfo paramInfo = CommandParamInfo.builder().tableInfoList(tables).commitBatchSize(batchLimit).snapshotBlockHeight(snaphotBlockHeight).build();

        MigrateState state = shardEngine.exportCsv(paramInfo);

        assertEquals(MigrateState.CSV_EXPORT_FINISHED, state);
        verify(csvExporter, times(2)).getDataExportPath();
        verify(csvExporter, times(1)).exportDerivedTable(goodsTable, snaphotBlockHeight, batchLimit);
        verify(csvExporter, times(1)).exportDerivedTable(goodsTable, snaphotBlockHeight, batchLimit, Set.of("DB_ID", "LATEST"));
        verifyNoMoreInteractions(csvExporter);
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
        assertEquals(2, recovery.getShardRecoveryId());
        assertEquals(MigrateState.CSV_EXPORT_FINISHED, recovery.getState());
    }

    @Test
    void testExportTablesWhenRecoveryInfoNotExistAndExistOldCsvAndNotCsvFiles() throws IOException {
        BlockTestData btd = new BlockTestData();
        blockchain.setLastBlock(btd.BLOCK_12);
        int snaphotBlockHeight = btd.BLOCK_10.getHeight() - 1;
        int batchLimit = 1;
        List<TableInfo> tables = List.of(new TableInfo(GOODS_TABLE_NAME));
        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        CommandParamInfo paramInfo = CommandParamInfo.builder().tableInfoList(tables).commitBatchSize(batchLimit).snapshotBlockHeight(snaphotBlockHeight).build();
        Path csvFile = Files.createFile(dataExportDirPath.resolve("old.csv"));
        Path emptyDir = Files.createDirectory(dataExportDirPath.resolve("empty-dir"));
        Path directory = Files.createDirectory(dataExportDirPath.resolve("export-dir"));
        Path txtFile = directory.resolve("old.txt");
        Files.createFile(txtFile);
        Path anotherCsvFile = Files.createFile(directory.resolve("another.csv"));

        MigrateState state = shardEngine.exportCsv(paramInfo);

        assertEquals(MigrateState.CSV_EXPORT_FINISHED, state);
        verify(csvExporter, times(2)).getDataExportPath();
        verify(csvExporter, times(1)).exportDerivedTable(goodsTable, snaphotBlockHeight, batchLimit);
        verify(csvExporter, times(1)).exportDerivedTable(goodsTable, snaphotBlockHeight, batchLimit, Set.of("DB_ID", "LATEST"));

        verifyNoMoreInteractions(csvExporter);
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
        assertEquals(2, recovery.getShardRecoveryId());
        assertEquals(MigrateState.CSV_EXPORT_FINISHED, recovery.getState());

        assertTrue(Files.exists(emptyDir));
        assertTrue(Files.exists(txtFile));
        assertFalse(Files.exists(csvFile));
        assertTrue(Files.exists(anotherCsvFile));
        assertTrue(Files.exists(directory));
    }

    @Test
    void testArchiveCsv() throws IOException, SQLException {
        Files.createFile(dataExportDirPath.resolve(GOODS_TABLE_NAME + CSV_FILE_EXTENSION));
        Files.createFile(dataExportDirPath.resolve(PRUNABLE_MESSAGE_TABLE_NAME + CSV_FILE_EXTENSION));
        Files.createFile(dataExportDirPath.resolve("another-file.txt"));
        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        CommandParamInfo paramInfo = CommandParamInfo.builder().shardId(4L).tableInfoList(List.of(new TableInfo(PRUNABLE_MESSAGE_TABLE_NAME, true), new TableInfo(GOODS_TABLE_NAME))).build();

        MigrateState state = shardEngine.archiveCsv(paramInfo);

        assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, state);
        Path coreZip = dataExportDirPath.resolve("apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip");
        verifyZip(coreZip, GOODS_TABLE_NAME + CSV_FILE_EXTENSION);
        Path prunableZip = dataExportDirPath.resolve("apl-blockchain-shardprun-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip");
        verifyZip(prunableZip, PRUNABLE_MESSAGE_TABLE_NAME + CSV_FILE_EXTENSION);

        Shard lastShard = shardDao.getLastShard();
        assertNotNull(lastShard.getCoreZipHash());
        assertNotNull(lastShard.getPrunableZipHash());
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
        assertNull(recovery.getProcessedObject());
        assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, recovery.getState());
    }

    void verifyZip(Path path, String... files) throws IOException {
        assertTrue(Files.exists(path));
        Path output = dataExportDirPath.resolve("output-" + path.getFileName());
        zip.extract(path.toAbsolutePath().toString(), output.toAbsolutePath().toString());
        assertEquals(files.length, FileUtils.countElementsOfDirectory(output));
        for (String file : files) {
            Files.exists(output.resolve(file));
        }
    }

    @Test
    void testArchiveCsvWithException() throws IOException {
        Files.createFile(dataExportDirPath.resolve(GOODS_TABLE_NAME + CSV_FILE_EXTENSION));
        Files.createFile(dataExportDirPath.resolve(PRUNABLE_MESSAGE_TABLE_NAME + CSV_FILE_EXTENSION));
        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        TableInfo prunableTableInfo = spy(new TableInfo(PRUNABLE_MESSAGE_TABLE_NAME, true));
        CommandParamInfo paramInfo = CommandParamInfo.builder().shardId(4L).tableInfoList(List.of(prunableTableInfo, new TableInfo(GOODS_TABLE_NAME))).build();
        doThrow(new RuntimeException()).when(prunableTableInfo).getName();

        MigrateState state = shardEngine.archiveCsv(paramInfo);

        assertEquals(MigrateState.FAILED, state);
        assertTrue(Files.exists(dataExportDirPath.resolve("apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip")));
        assertFalse(Files.exists(dataExportDirPath.resolve("apl-blockchain-shardprun-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip")));
        Shard lastShard = shardDao.getLastShard();
        assertNotNull(lastShard.getCoreZipHash());
        assertNull(lastShard.getPrunableZipHash());
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
        assertEquals("apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip", recovery.getProcessedObject());
        assertEquals(MigrateState.ZIP_ARCHIVE_STARTED, recovery.getState());
    }

    @Test
    void testArchiveAlreadyFinished() {
        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        CommandParamInfo paramInfo = CommandParamInfo.builder().shardId(4L).tableInfoList(List.of(new TableInfo(PRUNABLE_MESSAGE_TABLE_NAME, true), new TableInfo(GOODS_TABLE_NAME))).build();
        shardRecoveryDaoJdbc.saveShardRecovery(extension.getDatabaseManager().getDataSource(), new ShardRecovery(MigrateState.ZIP_ARCHIVE_FINISHED));

        MigrateState state = shardEngine.archiveCsv(paramInfo);

        assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, state);
        assertFalse(Files.exists(dataExportDirPath.resolve("apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip")));
        assertFalse(Files.exists(dataExportDirPath.resolve("apl-blockchain-shardprun-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip")));
    }

    @Test
    void testArchiveFromRecovery() throws IOException {
        Files.createFile(dataExportDirPath.resolve(GOODS_TABLE_NAME + CSV_FILE_EXTENSION));
        Files.createFile(dataExportDirPath.resolve(PRUNABLE_MESSAGE_TABLE_NAME + CSV_FILE_EXTENSION));
        Files.createFile(dataExportDirPath.resolve("another-file.txt"));
        Files.createFile(dataExportDirPath.resolve("apl-blockchain-shardprun-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip"));

        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));
        shardRecoveryDaoJdbc.saveShardRecovery(extension.getDatabaseManager().getDataSource(), new ShardRecovery(MigrateState.ZIP_ARCHIVE_STARTED, null, null, null, "apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip"));

        CommandParamInfo paramInfo = CommandParamInfo.builder().shardId(4L).tableInfoList(List.of(new TableInfo(PRUNABLE_MESSAGE_TABLE_NAME, true), new TableInfo(GOODS_TABLE_NAME))).build();

        MigrateState state = shardEngine.archiveCsv(paramInfo);

        assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, state);
        Path coreZip = dataExportDirPath.resolve("apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip");
        assertFalse(Files.exists(coreZip));
        Path prunableZip = dataExportDirPath.resolve("apl-blockchain-shardprun-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip");
        verifyZip(prunableZip, PRUNABLE_MESSAGE_TABLE_NAME + CSV_FILE_EXTENSION);

        Shard lastShard = shardDao.getLastShard();
        assertNull(lastShard.getCoreZipHash());
        assertNotNull(lastShard.getPrunableZipHash());
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
        assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, recovery.getState());
    }

    @Test
    void testArchiveWithoutPrunableData() throws IOException {
        Files.createFile(dataExportDirPath.resolve(GOODS_TABLE_NAME + CSV_FILE_EXTENSION));
        Files.createFile(dataExportDirPath.resolve("another-file.txt"));
        Files.createFile(dataExportDirPath.resolve("apl-blockchain-shardprun-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip"));

        DbUtils.inTransaction(extension, (con)-> shardRecoveryDaoJdbc.hardDeleteAllShardRecovery(con));

        CommandParamInfo paramInfo = CommandParamInfo.builder().shardId(4L).tableInfoList(List.of(new TableInfo(PRUNABLE_MESSAGE_TABLE_NAME, true), new TableInfo("goods"))).build();

        MigrateState state = shardEngine.archiveCsv(paramInfo);

        assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, state);
        Path coreZip = dataExportDirPath.resolve("apl-blockchain-shard-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip");
        verifyZip(coreZip, GOODS_TABLE_NAME + CSV_FILE_EXTENSION);
        Path prunableZip = dataExportDirPath.resolve("apl-blockchain-shardprun-4-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip");
        assertFalse(Files.exists(prunableZip));

        Shard lastShard = shardDao.getLastShard();
        assertNotNull(lastShard.getCoreZipHash());
        assertNull(lastShard.getPrunableZipHash());
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
        assertEquals(MigrateState.ZIP_ARCHIVE_FINISHED, recovery.getState());
    }
}