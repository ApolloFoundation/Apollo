/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.DATA_TAG_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PRUNABLE_MESSAGE_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.SHUFFLING_DATA_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_SHARD_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_TABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfoImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

@EnableWeld
class DataTransferManagementReceiverTest {
    private static final Logger log = getLogger(DataTransferManagementReceiverTest.class);

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("targetDb").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ReferencedTransactionDao.class,
            DerivedDbTablesRegistryImpl.class,
            TransactionTestData.class, PropertyProducer.class, ShardRecoveryDaoJdbcImpl.class,
            GlobalSyncImpl.class, FullTextConfigImpl.class, FullTextConfig.class,
            DataTransferManagementReceiverImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class, TrimService.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();

    private static PropertiesHolder propertiesHolder;

    @Inject
    private JdbiHandleFactory jdbiHandleFactory;
    @Inject
    private DataTransferManagementReceiver managementReceiver;
    @Inject
    private BlockIndexDao blockIndexDao;
    @Inject
    private TransactionIndexDao transactionIndexDao;

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @AfterEach
    void tearDown() {
        jdbiHandleFactory.close();
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
        TransactionTestData td = new TransactionTestData();
        Set<Long> dbIds = new HashSet<>();
        dbIds.add(td.DB_ID_0);
        dbIds.add(td.DB_ID_3);
        dbIds.add(td.DB_ID_10);
        CommandParamInfo paramInfo = new CommandParamInfoImpl(tableNameList, 2, 8000L, dbIds);

        state = managementReceiver.copyDataToShard(paramInfo);
        assertEquals(MigrateState.DATA_COPIED_TO_SHARD, state);
//        assertEquals(MigrateState.FAILED, state);

        state = managementReceiver.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
        assertEquals(SHARD_SCHEMA_FULL, state);

        tableNameList.clear();
//        tableNameList.add(GENESIS_PUBLIC_KEY_TABLE_NAME);
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
        long blockIndexCount = blockIndexDao.countBlockIndexByShard(4L);
        assertEquals(8, blockIndexCount);
        long trIndexCount = transactionIndexDao.countTransactionIndexByShardId(4L);
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
// compare fullhashes
        TransactionIndex index = transactionIndexDao.getByTransactionId(td.TRANSACTION_1.getId());
        assertNotNull(index);
        byte[] fullHash = Convert.toFullHash(index.getTransactionId(), index.getPartialTransactionHash());
        assertArrayEquals(td.TRANSACTION_1.getFullHash(), fullHash);
        log.debug("Migration finished in = {} sec", (System.currentTimeMillis() - start)/1000 );
    }
}