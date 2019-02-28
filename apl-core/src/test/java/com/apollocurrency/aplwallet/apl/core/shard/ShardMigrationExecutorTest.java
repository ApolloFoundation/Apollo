package com.apollocurrency.aplwallet.apl.core.shard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CreateTempDbCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DbFilesRenameCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.MoveDataCommand;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
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
import org.junit.jupiter.api.Test;

@EnableWeld
class ShardMigrationExecutorTest {

    private static String BASE_SUB_DIR = "unit-test-db";

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesConfigLoader.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManager.class, DataTransferManagementReceiverImpl.class,
            ShardMigrationExecutor.class)
            .build();

    private static Path pathToDb;
    private static PropertiesHolder propertiesHolder;
    private static DbProperties dbProperties;
    private static DatabaseManager databaseManager;
    private static DataTransferManagementReceiver transferManagementReceiver;
    @Inject
    private ShardMigrationExecutor shardMigrationExecutor;

    @BeforeAll
    static void setUpAll() {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwx---");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        String workingDir = System.getProperty("user.dir");
        // path to temporary databaseManager inside project
        pathToDb = FileSystems.getDefault().getPath(workingDir + File.separator  + BASE_SUB_DIR);

        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                configDirProvider,
                false,
                "./" + BASE_SUB_DIR,
                Constants.APPLICATION_DIR_NAME + ".properties",
                Collections.emptyList());
        propertiesHolder = new PropertiesHolder();
        propertiesHolder.init(propertiesLoader.load());
        DbConfig dbConfig = new DbConfig(propertiesHolder);
        dbProperties = dbConfig.getDbConfig();
        databaseManager = new DatabaseManager(dbProperties, propertiesHolder);
        transferManagementReceiver = new DataTransferManagementReceiverImpl(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.shutdown();
        FileUtils.deleteQuietly(pathToDb.toFile());
    }

    @Test
    void executeCreateTempDbOperation() throws IOException {
        assertNotNull(databaseManager);
        CreateTempDbCommand tempDbCommand = new CreateTempDbCommand(transferManagementReceiver);
        MigrateState state = shardMigrationExecutor.executeOperation(tempDbCommand);
        assertNotNull(state);
        assertEquals(MigrateState.TEMP_DB_CREATED, state);
    }

    @Test
    void executeAllOperations() throws IOException {
        assertNotNull(databaseManager);
        CreateTempDbCommand tempDbCommand = new CreateTempDbCommand(transferManagementReceiver);
        MigrateState state = shardMigrationExecutor.executeOperation(tempDbCommand);
        assertNotNull(state);
        assertEquals(MigrateState.TEMP_DB_CREATED, state);

        Map<String, Long> tableNameCountMap = new LinkedHashMap<>(0);
        tableNameCountMap.put("ACCOUNT", -1L);
        MoveDataCommand moveDataCommand = new MoveDataCommand(
                transferManagementReceiver, tableNameCountMap, null, Collections.emptyList(), -1);
        state = shardMigrationExecutor.executeOperation(moveDataCommand);
        assertEquals(MigrateState.DATA_MOVING, state);

        DbFilesRenameCommand dbFilesRenameCommand = new DbFilesRenameCommand(
                transferManagementReceiver, null, Collections.emptyList(), -1);
        state = shardMigrationExecutor.executeOperation(dbFilesRenameCommand);
        assertEquals(MigrateState.COMPLETED, state);
    }
}