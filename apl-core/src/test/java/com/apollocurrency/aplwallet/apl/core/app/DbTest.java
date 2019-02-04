package com.apollocurrency.aplwallet.apl.core.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;
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

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbProperties;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDb;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@EnableWeld
class DbTest {

    private static String BASE_SUB_DIR = "unit-test-db";
    private static String DB_FILE_NAME = "test_db";

    private DbProperties baseDbProperties;
    private Path pathToDb;
    @Inject
    private Db db;
    @Inject
    private PropertiesHolder propertiesHolder;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(Db.class, DbProperties.class, ConnectionProviderImpl.class, NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class,
            Time.EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .build();

    @BeforeEach
    void setUp() throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        String workingDir = System.getProperty("user.dir");
        // path to temporary db inside project
        Path currentPath = FileSystems.getDefault().getPath(workingDir + File.separator  + BASE_SUB_DIR);
        // check or create database folder
        if (!Files.exists(currentPath)) {
            Files.createDirectory(currentPath, attr);
        }
        String dbFileName = DB_FILE_NAME + ".h2";
        Path dbFile = currentPath.toAbsolutePath().resolve(dbFileName);
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

    @Test
    void init() throws Exception {
        Db.init(baseDbProperties);
        Db.shutdown();
        Files.deleteIfExists(pathToDb);
    }

    @Test
    void createAndAddShard() throws Exception {
        db.init(baseDbProperties);
        TransactionalDb newShardDb = db.createAndAddShard("apl-shard-000001");
        assertNotNull(newShardDb);
        assertNotNull(newShardDb.getConnection());
        newShardDb.shutdown();
        Db.shutdown();
        Files.deleteIfExists(pathToDb);
    }
}