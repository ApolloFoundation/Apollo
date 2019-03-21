/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.COMPLETED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_COPIED_TO_SHARD;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_RELINKED_IN_MAIN;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_REMOVED_FROM_MAIN;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SECONDARY_INDEX_UPDATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbExtension;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CopyDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CreateShardSchemaCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DeleteCopiedDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.FinishShardingCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.ReLinkDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.UpdateSecondaryIndexCommand;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@Disabled
@EnableWeld
class ShardMigrationExecutorTest {
    private static final Logger log = getLogger(ShardMigrationExecutorTest.class);

    private static String BASE_SUB_DIR = "unit-test-db";
    private static Path pathToDb = FileSystems.getDefault().getPath(System.getProperty("user.dir") + File.separator  + BASE_SUB_DIR);;

    @RegisterExtension
    DbExtension extension = new DbExtension(baseDbProperties, propertiesHolder);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, TransactionImpl.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ReferencedTransactionDao.class,
            TransactionTestData.class, PropertyProducer.class,
            GlobalSyncImpl.class, BlockIndexDao.class, ShardingHashCalculatorImpl.class,
            DerivedDbTablesRegistry.class, DataTransferManagementReceiverImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class, TrimService.class)
/*
            PropertiesHolder.class, TransactionImpl.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ReferencedTransactionDao.class,
            TransactionTestData.class, PropertyProducer.class,
            GlobalSyncImpl.class,
            DerivedDbTablesRegistry.class, DataTransferManagementReceiverImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class, TrimService.class)
*/
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(baseDbProperties, DbProperties.class))
            .build();


    private static PropertiesHolder propertiesHolder;
    private static DbProperties baseDbProperties;

    @Inject
    private JdbiHandleFactory jdbiHandleFactory;
    @Inject
    private DataTransferManagementReceiver managementReceiver;
    @Inject
    private DerivedDbTablesRegistry dbTablesRegistry;
    @Inject
    private Event<MigrateState> migrateStateEvent;
    @Inject
    BlockIndexDao blockIndexDao;
    @Inject
    ShardingHashCalculator shardingHashCalculator;
    @Inject
    private ShardMigrationExecutor shardMigrationExecutor;

    @BeforeAll
    static void setUpAll() {
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                null,
                false,
                null,
                Constants.APPLICATION_DIR_NAME + ".properties",
                Collections.emptyList());
        propertiesHolder = new PropertiesHolder();
        propertiesHolder.init(propertiesLoader.load());
        DbConfig dbConfig = new DbConfig(propertiesHolder);
        baseDbProperties = dbConfig.getDbConfig();
    }

    @BeforeEach
    void setUp() {
        PublicKeyTable publicKeyTable = PublicKeyTable.getInstance();
        dbTablesRegistry.registerDerivedTable(publicKeyTable);
    }

    @AfterEach
    void tearDown() {
        jdbiHandleFactory.close();
        FileUtils.deleteQuietly(pathToDb.toFile());
    }

/*
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
        shardMigrationExecutor = new ShardMigrationExecutor(transferManagementReceiver, migrateStateEvent, shardingHashCalculator, blockIndexDao);
    }
*/

/*
    @AfterEach
    void tearDownAll() {
        FileUtils.deleteQuietly(pathToDb.toFile());
    }
*/

    @Test
    void executeAllOperations() throws IOException {
//        assertNotNull(databaseManager);

        CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(managementReceiver,
                new ShardInitTableSchemaVersion());
        MigrateState state = shardMigrationExecutor.executeOperation(createShardSchemaCommand);
        assertEquals(SHARD_SCHEMA_CREATED, state);

        CopyDataCommand copyDataCommand = new CopyDataCommand(
                managementReceiver, 8000L);
        state = shardMigrationExecutor.executeOperation(copyDataCommand);
//        assertEquals(FAILED, state);
        assertEquals(DATA_COPIED_TO_SHARD, state);

        createShardSchemaCommand = new CreateShardSchemaCommand(managementReceiver,
                new ShardAddConstraintsSchemaVersion());
        state = shardMigrationExecutor.executeOperation(createShardSchemaCommand);
        assertEquals(SHARD_SCHEMA_FULL, state);

        ReLinkDataCommand reLinkDataCommand = new ReLinkDataCommand(managementReceiver, 8000L);
        state = shardMigrationExecutor.executeOperation(reLinkDataCommand);
        assertEquals(DATA_RELINKED_IN_MAIN, state);

        UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand(managementReceiver, 8000L);
        state = shardMigrationExecutor.executeOperation(updateSecondaryIndexCommand);
//        assertEquals(FAILED, state);
        assertEquals(SECONDARY_INDEX_UPDATED, state);

        DeleteCopiedDataCommand deleteCopiedDataCommand = new DeleteCopiedDataCommand(managementReceiver, 8000L);
        state = shardMigrationExecutor.executeOperation(deleteCopiedDataCommand);
//        assertEquals(FAILED, state);
        assertEquals(DATA_REMOVED_FROM_MAIN, state);

        FinishShardingCommand finishShardingCommand = new FinishShardingCommand(managementReceiver, new byte[]{3,4,5,6,1});
        state = shardMigrationExecutor.executeOperation(finishShardingCommand);
        assertEquals(COMPLETED, state);

    }

    @Test
    void executeAll() {
//        assertNotNull(databaseManager);
        shardMigrationExecutor.createAllCommands(0);
        MigrateState state = shardMigrationExecutor.executeAllOperations();
//        assertEquals(FAILED, state);
        assertEquals(COMPLETED, state);
    }
}