package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver.TEMPORARY_MIGRATION_FILE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.TEMP_DB_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.enterprise.inject.spi.CDI;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigProducer;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Consensus;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class DataTransferManagementReceiverTest {

    private static String BASE_SUB_DIR = "unit-test-db";
    private static String TEMP_FILE_NAME = "apl-temp-db-name";

//    @Mock
//    private FullTextConfigProducer fullTextConfigProducer = createTextConfigProducer();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesConfigLoader.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManager.class, DataTransferManagementReceiverImpl.class/*,
            FullTextSearchServiceImpl.class, LuceneFullTextSearchEngine.class*/)
//            .addBeans(MockBean.of(fullTextConfigProducer, FullTextConfigProducer.class))
            .build();

    private static Path pathToDb;
    private static PropertiesHolder propertiesHolder;
    private static DbProperties baseDbProperties;
    private static DatabaseManager databaseManager;
    private DataTransferManagementReceiver transferManagementReceiver;
    private Blockchain blockchain;

    @BeforeAll
    static void setUpAll() {
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);
        String workingDir = System.getProperty("user.dir");
        pathToDb = FileSystems.getDefault().getPath(workingDir + File.separator  + BASE_SUB_DIR);
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                configDirProvider,
                false,
                "./" + BASE_SUB_DIR,
                Constants.APPLICATION_DIR_NAME + ".properties",
                Collections.emptyList());
        propertiesHolder = new PropertiesHolder();
        propertiesHolder.init(propertiesLoader.load());
        DbConfig dbConfig = new DbConfig(propertiesHolder);
        baseDbProperties = dbConfig.getDbConfig();
        databaseManager = new DatabaseManager(baseDbProperties, propertiesHolder);
    }

    @BeforeEach
    void setUp() {
        blockchain = CDI.current().select(BlockchainImpl.class).get();
        transferManagementReceiver = new DataTransferManagementReceiverImpl(databaseManager, blockchain);
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        BlockchainProperties blockchainProperties = new BlockchainProperties(
                10, 10, 10, 10, 10, 10L, new Consensus());
        HeightConfig heightConfig = new HeightConfig(blockchainProperties);
        blockchainConfig.setCurrentConfig(heightConfig);
    }

    private FullTextConfigProducer createTextConfigProducer() {
//        FullTextConfigProducer producer = new FullTextConfigProducer();
        FullTextConfigProducer producer = mock(FullTextConfigProducer.class);
        when(producer.produceIndexPath()).thenReturn(pathToDb);
        return producer;
    }

    @AfterEach
    void tearDown() {
        FileUtils.deleteQuietly(pathToDb.toFile());
    }

    @Test
    void createTemporaryDb() throws IOException {
        MigrateState state = transferManagementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        DatabaseMetaInfo databaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null,
                -1, TEMP_DB_CREATED, null);

        state = transferManagementReceiver.createTempDb(databaseMetaInfo);
        assertEquals(TEMP_DB_CREATED, state);

/*
        Path dbFile = pathToDb.toAbsolutePath().resolve(TEMP_FILE_NAME + ".h2.db");
        Path dbFile2 = pathToDb.toAbsolutePath().resolve(TEMP_FILE_NAME + ".trace.db");

        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(dbFile2);
*/
    }

    @Disabled
    @Test
    void createAddSnapshotBlock() throws IOException {
        MigrateState state = transferManagementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        DatabaseMetaInfo databaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null,
                -1, TEMP_DB_CREATED, null);

        state = transferManagementReceiver.createTempDb(databaseMetaInfo);
        assertEquals(TEMP_DB_CREATED, state);

        Block block = new BlockImpl(-1, 0, 0, 0, 0, 0,
        new byte[32], 0L, new byte[32], new byte[32], new byte[32], BigInteger.ONE, 1L, 0L, 0, 1, 0,
                Collections.emptyList());

/*
        Block block = new BlockImpl(-1, 0, 0, 0, 0, 0,
                new byte[32], new byte[32], new byte[32], new byte[64],
                new byte[32],  0, Collections.emptyList());
*/

        databaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null,
                -1, TEMP_DB_CREATED, block);

        state = transferManagementReceiver.addSnapshotBlock(databaseMetaInfo);
        assertEquals(MigrateState.SNAPSHOT_BLOCK_CREATED, state);

/*
        Path dbFile = pathToDb.toAbsolutePath().resolve(TEMP_FILE_NAME + ".h2.db");
        Path dbFile2 = pathToDb.toAbsolutePath().resolve(TEMP_FILE_NAME + ".trace.db");

        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(dbFile2);
*/
    }

    @Test
    void createTemporaryDbAndMoveDataFromMain() throws IOException {
        MigrateState state = transferManagementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        Map<String, Long> tableNameCountMap = new LinkedHashMap<>(10);
//        tableNameCountMap.put("BLOCK", -1L);
//        tableNameCountMap.put("GENESIS_PUBLIC_KEY", -1L);
        tableNameCountMap.put("ACCOUNT", -1L);
        tableNameCountMap.put("ACCOUNT_LEDGER", -1L);
        tableNameCountMap.put("ACCOUNT_INFO", -1L);
        tableNameCountMap.put("PURCHASE", -1L);
        tableNameCountMap.put("CURRENCY", -1L);

//        List<String> tableSelectedList = new ArrayList<>(100);
//        tableSelectedList.add("ACCOUNT_LEDGER");
        DatabaseMetaInfo tempDbMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null, 5000, MigrateState.DATA_MOVING_STARTED, null);

        state = transferManagementReceiver.createTempDb(tempDbMetaInfo);
        assertEquals(TEMP_DB_CREATED, state);

//        List<String> mainDbInsertList = new ArrayList<>(100);
//        mainDbInsertList.add("ACCOUNT_LEDGER");
        DatabaseMetaInfo mainDbMetaInfo = new DatabaseMetaInfoImpl(
                null, "apl-blockchain", null, 1000, MigrateState.DATA_MOVING_STARTED, null);

        state = transferManagementReceiver.moveData(tableNameCountMap, mainDbMetaInfo, tempDbMetaInfo);
        assertEquals(MigrateState.DATA_MOVING_STARTED, state);
    }
}