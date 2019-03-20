/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.COMPLETED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_RELINKED_IN_MAIN;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.FAILED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CopyDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CreateShardSchemaCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DeleteCopiedDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.FinishShardingCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.ReLinkDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.UpdateSecondaryIndexCommand;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@EnableWeld
class ShardMigrationExecutorTest {

    private static String BASE_SUB_DIR = "unit-test-db";

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesConfigLoader.class, PropertyProducer.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManagerImpl.class, DataTransferManagementReceiverImpl.class,
            ShardMigrationExecutor.class, GlobalSyncImpl.class, DerivedDbTablesRegistry.class, TrimService.class )
            .build();

    @Inject
    private GlobalSync globalSync;

    private static Path pathToDb;
    private static PropertiesHolder propertiesHolder;
    @Inject
    private PropertyProducer propertyProducer;
    private static DbProperties dbProperties;
    private static DbConfig dbConfig;
    private static DatabaseManager databaseManager;
    private DataTransferManagementReceiver transferManagementReceiver;
    @Inject
    private DerivedDbTablesRegistry dbTablesRegistry;
    private TrimService trimService;
    @Inject
    private Blockchain blockchain;
    @Inject
    private ShardMigrationExecutor shardMigrationExecutor;
    @Inject
    private Event<MigrateState> migrateStateEvent;

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
        dbConfig = new DbConfig(propertiesHolder);
        dbProperties = dbConfig.getDbConfig();
        databaseManager = new DatabaseManagerImpl(dbProperties, propertiesHolder);
    }

    @BeforeEach
    void setUp() {
        blockchain = CDI.current().select(BlockchainImpl.class).get();
        propertyProducer = new PropertyProducer(propertiesHolder);
        PublicKeyTable publicKeyTable = PublicKeyTable.getInstance();
        dbTablesRegistry.registerDerivedTable(publicKeyTable);
        trimService = new TrimService(false, 100,720, databaseManager, dbTablesRegistry, globalSync);
        transferManagementReceiver = new DataTransferManagementReceiverImpl(databaseManager, trimService);
        shardMigrationExecutor = new ShardMigrationExecutor(transferManagementReceiver, migrateStateEvent);
    }

    @AfterEach
    void tearDownAll() {
        FileUtils.deleteQuietly(pathToDb.toFile());
    }

    @Test
    void executeAllOperations() throws IOException {
        assertNotNull(databaseManager);

        CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(transferManagementReceiver,
                new ShardInitTableSchemaVersion());
        MigrateState state = shardMigrationExecutor.executeOperation(createShardSchemaCommand);
        assertEquals(SHARD_SCHEMA_CREATED, state);

        CopyDataCommand copyDataCommand = new CopyDataCommand(
                transferManagementReceiver, 1L);
        state = shardMigrationExecutor.executeOperation(copyDataCommand);
        assertEquals(FAILED /*DATA_COPIED_TO_SHARD*/, state);

        createShardSchemaCommand = new CreateShardSchemaCommand(transferManagementReceiver,
                new ShardAddConstraintsSchemaVersion());
        state = shardMigrationExecutor.executeOperation(createShardSchemaCommand);
        assertEquals(SHARD_SCHEMA_FULL, state);

        ReLinkDataCommand reLinkDataCommand = new ReLinkDataCommand(transferManagementReceiver, 0L);
        state = shardMigrationExecutor.executeOperation(reLinkDataCommand);
        assertEquals(DATA_RELINKED_IN_MAIN, state);

        UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand(transferManagementReceiver, 0L);
        state = shardMigrationExecutor.executeOperation(updateSecondaryIndexCommand);
        assertEquals(FAILED/*SECONDARY_INDEX_UPDATED*/, state);

        DeleteCopiedDataCommand deleteCopiedDataCommand = new DeleteCopiedDataCommand(transferManagementReceiver, 0L);
        state = shardMigrationExecutor.executeOperation(deleteCopiedDataCommand);
        assertEquals(FAILED/*DATA_REMOVED_FROM_MAIN*/, state);

        FinishShardingCommand finishShardingCommand = new FinishShardingCommand(transferManagementReceiver, new byte[]{3,4,5,6,1});
        state = shardMigrationExecutor.executeOperation(finishShardingCommand);
        assertEquals(COMPLETED, state);

    }

    @Test
    void executeAll() {
        assertNotNull(databaseManager);
        shardMigrationExecutor.createAllCommands(0);
        MigrateState state = shardMigrationExecutor.executeAllOperations();
        assertEquals(FAILED/*COMPLETED*/, state);
    }
}