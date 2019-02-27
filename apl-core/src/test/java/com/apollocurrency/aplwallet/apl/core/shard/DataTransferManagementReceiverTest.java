package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver.TEMPORARY_MIGRATION_FILE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.TEMP_DB_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@EnableWeld
class DataTransferManagementReceiverTest {

    private static String BASE_SUB_DIR = "unit-test-db";
    private static String TEMP_FILE_NAME = "apl-temp-db-name";

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesConfigLoader.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManager.class, DataTransferManagementReceiverImpl.class)
            .build();

    private static Path pathToDb;
    private static PropertiesHolder propertiesHolder;
    private static DbProperties baseDbProperties;
    private static DatabaseManager databaseManager;
    private static DataTransferManagementReceiver transferManagementReceiver;

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
        transferManagementReceiver = new DataTransferManagementReceiverImpl(databaseManager);
    }

/*
    @AfterEach
    void tearDown() {
        FileUtils.deleteQuietly(pathToDb.toFile());
    }
*/

    @Test
    void createTemporaryDb() throws IOException {
        MigrateState state = transferManagementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        DatabaseMetaInfo databaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null, -1, TEMP_DB_CREATED);

        state = transferManagementReceiver.createTempDb(databaseMetaInfo);
        assertEquals(TEMP_DB_CREATED, state);

        Path dbFile = pathToDb.toAbsolutePath().resolve(TEMP_FILE_NAME + ".h2.db");
        Path dbFile2 = pathToDb.toAbsolutePath().resolve(TEMP_FILE_NAME + ".trace.db");

        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(dbFile2);
    }

    @Test
    void createTemporaryDbAndMoveDataFromMain() throws IOException {
        MigrateState state = transferManagementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        List<String> tempDbSelectList = new ArrayList<>(100);
        tempDbSelectList.add("ACCOUNT_LEDGER");
        DatabaseMetaInfo tempDbMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null, 52, TEMP_DB_CREATED);

        state = transferManagementReceiver.createTempDb(tempDbMetaInfo);
        assertEquals(TEMP_DB_CREATED, state);

        List<String> mainDbInsertList = new ArrayList<>(100);
        mainDbInsertList.add("ACCOUNT_LEDGER");
        DatabaseMetaInfo mainDbMetaInfo = new DatabaseMetaInfoImpl(
                null, "apl-blockchain", mainDbInsertList, -1, MigrateState.DATA_MOVING);

        state = transferManagementReceiver.moveData(mainDbMetaInfo, tempDbMetaInfo);
        assertEquals(MigrateState.DATA_MOVING, state);
    }
}