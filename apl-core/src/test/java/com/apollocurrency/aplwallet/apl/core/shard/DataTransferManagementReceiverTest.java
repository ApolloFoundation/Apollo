/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.DATA_TAG_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.GENESIS_PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PRUNABLE_MESSAGE_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.SHUFFLING_DATA_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_SHARD_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_TABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfoImpl;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

@EnableWeld
class DataTransferManagementReceiverTest {
    private static final Logger log = getLogger(DataTransferManagementReceiverTest.class);

    private static String BASE_SUB_DIR = "unit-test-db";
    private static Path pathToDb = FileSystems.getDefault().getPath(System.getProperty("user.dir") + File.separator  + BASE_SUB_DIR);;

    @RegisterExtension
    DbExtension extension = new DbExtension(baseDbProperties, propertiesHolder);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, TransactionImpl.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ReferencedTransactionDao.class,
            TransactionTestData.class, PropertyProducer.class, ShardRecoveryDao.class,
            GlobalSyncImpl.class,
            DerivedDbTablesRegistry.class, DataTransferManagementReceiverImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class, TrimService.class)
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
    private BlockIndexDao blockIndexDao;
    @Inject
    private TransactionIndexDao transactionIndexDao;

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

    @AfterEach
    void tearDown() {
        jdbiHandleFactory.close();
        extension.getDatabaseManger().shutdown();
//        FileUtils.deleteQuietly(pathToDb.toFile());
    }

    @Test
    void createShardDb() throws IOException {
        MigrateState state = managementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);
        state = managementReceiver.addOrCreateShard(new ShardInitTableSchemaVersion());
        assertEquals(SHARD_SCHEMA_CREATED, state);
    }

    @Test
    void createFullShardDb() throws IOException {
        MigrateState state = managementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        state = managementReceiver.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
        assertEquals(SHARD_SCHEMA_FULL, state);
    }

    @Test
    void createShardDbDoAllOperations() throws IOException {
        long start = System.currentTimeMillis();
        MigrateState state = managementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        state = managementReceiver.addOrCreateShard(new ShardInitTableSchemaVersion());
        assertEquals(SHARD_SCHEMA_CREATED, state);

        List<String> tableNameList = new ArrayList<>();
        tableNameList.add(BLOCK_TABLE_NAME);
        tableNameList.add(TRANSACTION_TABLE_NAME);
        CommandParamInfo paramInfo = new CommandParamInfoImpl(tableNameList, 2, 8000L);

        state = managementReceiver.copyDataToShard(paramInfo);
        assertEquals(MigrateState.DATA_COPIED_TO_SHARD, state);
//        assertEquals(MigrateState.FAILED, state);

        state = managementReceiver.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
        assertEquals(SHARD_SCHEMA_FULL, state);

        tableNameList.clear();
        tableNameList.add(GENESIS_PUBLIC_KEY_TABLE_NAME);
        tableNameList.add(PUBLIC_KEY_TABLE_NAME);
//        tableNameList.add(TAGGED_DATA_TABLE_NAME); // ! skip in test
        tableNameList.add(SHUFFLING_DATA_TABLE_NAME);
        tableNameList.add(DATA_TAG_TABLE_NAME);
        tableNameList.add(PRUNABLE_MESSAGE_TABLE_NAME);

        paramInfo.setTableNameList(tableNameList);
        state = managementReceiver.relinkDataToSnapshotBlock(paramInfo);
        assertEquals(MigrateState.DATA_RELINKED_IN_MAIN, state);
//        assertEquals(MigrateState.FAILED, state);

        tableNameList.clear();
        tableNameList.add(BLOCK_INDEX_TABLE_NAME);
        tableNameList.add(TRANSACTION_SHARD_INDEX_TABLE_NAME);

        paramInfo.setTableNameList(tableNameList);
        state = managementReceiver.updateSecondaryIndex(paramInfo);
        assertEquals(MigrateState.SECONDARY_INDEX_UPDATED, state);
//        assertEquals(MigrateState.FAILED, state);
        long blockIndexCount = blockIndexDao.countBlockIndexByShard(3L);
        assertEquals(8, blockIndexCount);
        long trIndexCount = transactionIndexDao.countTransactionIndexByShardId(3L);
        assertEquals(5, trIndexCount);

        tableNameList.clear();
        tableNameList.add(BLOCK_TABLE_NAME);

        paramInfo.setTableNameList(tableNameList);
        state = managementReceiver.deleteCopiedData(paramInfo);
        assertEquals(MigrateState.DATA_REMOVED_FROM_MAIN, state);
//        assertEquals(MigrateState.FAILED, state);

        paramInfo.setShardHash("000000000".getBytes());
        state = managementReceiver.addShardInfo(paramInfo);
        assertEquals(MigrateState.COMPLETED, state);

        log.debug("Migration finished in = {} sec", (System.currentTimeMillis() - start)/1000 );
    }
}