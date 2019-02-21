package com.apollocurrency.aplwallet.apl.core.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;

@EnableWeld
class DatabaseManagerTest {

    private static String BASE_SUB_DIR = "unit-test-db";
    private static String DB_FILE_NAME = "test_db";

    private DbProperties baseDbProperties;
    private Path pathToDb;
    private Path pathToDbFolder;

//    @Inject
    private static DatabaseManager databaseManager;
    @Inject
    private PropertiesHolder propertiesHolder;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManager.class)
            .build();

    @BeforeEach
    void setUp() throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwx---");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        String workingDir = System.getProperty("user.dir");
        // path to temporary databaseManager inside project
        Path currentPath = FileSystems.getDefault().getPath(workingDir + File.separator  + BASE_SUB_DIR);
        // check or create database folder
        if (!Files.exists(currentPath)) {
            this.pathToDbFolder = Files.createDirectory(currentPath, attr);
        } else {
            this.pathToDbFolder = currentPath;
        }
        String dbFileName = DB_FILE_NAME + ".h2";
        Path dbFile = currentPath.toAbsolutePath().resolve(dbFileName);
        // check and create H2 DB file
        if (!Files.exists(dbFile)) {
            this.pathToDb = Files.createFile(dbFile);
        } else {
            this.pathToDb = dbFile;
        }
        Properties properties = new Properties();
        properties.put("apl.statementLogThreshold", 1000);
        properties.put("apl.transactionLogThreshold", 5000);
        properties.put("apl.transactionLogInterval", 456000);
        properties.put("apl.enableSqlLogs", true);
        propertiesHolder.init(properties);

        baseDbProperties = new DbProperties()
                .dbDir("./" + BASE_SUB_DIR)
                .dbFileName(DB_FILE_NAME)
                .dbPassword("sa")
                .dbUsername("sa")
                .dbType("h2")
                .loginTimeout(1000 * 30)
                .maxMemoryRows(100000)
                .dbParams("")
                .maxConnections(100)
                .maxCacheSize(0);

    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteQuietly(pathToDb.toFile());
        FileUtils.deleteQuietly(pathToDbFolder.toFile());
    }

    @AfterAll
    static void stopAll() {
        databaseManager.shutdown();
    }

    @Test
    void init() throws Exception {
        databaseManager = new DatabaseManager(baseDbProperties, propertiesHolder);
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
//        databaseManager.shutdown();
    }

    @Test
    void createAndAddShard() throws Exception {
        databaseManager = new DatabaseManager(baseDbProperties, propertiesHolder);
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = databaseManager.createAndAddShard("apl-shard-000001");
        assertNotNull(newShardDb);
        assertNotNull(newShardDb.getConnection());
//        newShardDb.shutdown(); // not needed
//        databaseManager.shutdown();
    }

}