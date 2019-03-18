/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfoImpl;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ConsensusSettings;
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
import org.slf4j.Logger;

@Disabled
@EnableWeld
@ExtendWith(MockitoExtension.class)
class DataTransferManagementReceiverTest {
    private static final Logger log = getLogger(DataTransferManagementReceiverTest.class);

    private static String BASE_SUB_DIR = "unit-test-db";

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesConfigLoader.class, GlobalSyncImpl.class, PropertyProducer.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManager.class,
            BlockchainConfig.class, BlockchainConfigUpdater.class, DerivedDbTablesRegistry.class, TrimService.class)
            .build();

    @Inject
    private GlobalSync globalSync;

    private static Path pathToDb;
    private static PropertiesHolder propertiesHolder;
    @Inject
    private PropertyProducer propertyProducer;
    private static DbProperties baseDbProperties;
    private static DatabaseManager databaseManager;
    private DataTransferManagementReceiver transferManagementReceiver;
    private Blockchain blockchain;
    @Inject
    private DerivedDbTablesRegistry dbTablesRegistry;
    @Inject
    private TrimService trimService;

    @BeforeAll
    static void setUpAll() {
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);
        String workingDir = System.getProperty("user.dir");
        pathToDb = FileSystems.getDefault().getPath(workingDir + File.separator  + BASE_SUB_DIR);
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                null,
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
        propertyProducer = new PropertyProducer(propertiesHolder);
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        BlockchainProperties blockchainProperties = new BlockchainProperties(
                10, 10, 10, 10, 10, 10L, new ConsensusSettings());
        HeightConfig heightConfig = new HeightConfig(blockchainProperties);
        blockchainConfig.setCurrentConfig(heightConfig);
        Chain chain = new Chain();
        UUID chainId = UUID.randomUUID();
        chain.setChainId(chainId);
        chain.setName(chainId.toString());
        chain.setDescription(chainId.toString());
        chain.setGenesisLocation("genesisAccounts-junit.json");
        Map<Integer, BlockchainProperties> blockchainPropertiesMap = new HashMap<>(1);
        blockchainPropertiesMap.put(0, blockchainProperties);
        chain.setBlockchainProperties(blockchainPropertiesMap);
        blockchainConfig.updateChain(chain, 10);
        PublicKeyTable publicKeyTable = PublicKeyTable.getInstance();
        dbTablesRegistry.registerDerivedTable(publicKeyTable);
        trimService = new TrimService(false, 100,720, databaseManager, dbTablesRegistry, globalSync);
        transferManagementReceiver = new DataTransferManagementReceiverImpl(databaseManager, trimService);
    }

    @AfterEach
    void tearDown() {
        FileUtils.deleteQuietly(pathToDb.toFile());
    }

    @Test
    void createShardDb() throws IOException {
        MigrateState state = transferManagementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);
        state = transferManagementReceiver.addOrCreateShard(new ShardInitTableSchemaVersion());
        assertEquals(SHARD_SCHEMA_CREATED, state);
    }

    @Test
    void createFullShardDb() throws IOException {
        MigrateState state = transferManagementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        state = transferManagementReceiver.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
        assertEquals(SHARD_SCHEMA_CREATED, state);
    }

    @Test
    void createShardDbAndMoveDataFromMain() throws IOException {
        long start = System.currentTimeMillis();
        MigrateState state = transferManagementReceiver.getCurrentState();
        assertNotNull(state);
        assertEquals(MigrateState.INIT, state);

        state = transferManagementReceiver.addOrCreateShard(new ShardInitTableSchemaVersion());
        assertEquals(SHARD_SCHEMA_CREATED, state);

        List<String> tableNameList = new LinkedList<>();
        tableNameList.add("BLOCK");
        tableNameList.add("TRANSACTION");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(tableNameList, 100, 1350000L);

        state = transferManagementReceiver.copyDataToShard(paramInfo);
//        assertEquals(MigrateState.DATA_COPIED_TO_SHARD, state);
        assertEquals(MigrateState.FAILED, state);

        state = transferManagementReceiver.addOrCreateShard(new ShardAddConstraintsSchemaVersion());
        assertEquals(SHARD_SCHEMA_FULL, state);

        tableNameList.clear();
        tableNameList.add("GENESIS_PUBLIC_KEY");
        tableNameList.add("PUBLIC_KEY");
//        tableNameList.put("TAGGED_DATA", -1L); // !
        tableNameList.add("SHUFFLING_DATA");
        tableNameList.add("DATA_TAG");
        tableNameList.add("PRUNABLE_MESSAGE");

        paramInfo.setTableNameList(tableNameList);
        state = transferManagementReceiver.relinkDataToSnapshotBlock(paramInfo);
        assertEquals(MigrateState.DATA_RELINKED_IN_MAIN, state);
//        assertEquals(MigrateState.FAILED, state);

        tableNameList.clear();
        tableNameList.add("BLOCK_INDEX");
        tableNameList.add("TRANSACTION_SHARD_INDEX");

        paramInfo.setTableNameList(tableNameList);
        state = transferManagementReceiver.updateSecondaryIndex(paramInfo);
//        assertEquals(MigrateState.SECONDARY_INDEX_UPDATED, state);
        assertEquals(MigrateState.FAILED, state);

        tableNameList.clear();
        tableNameList.add("BLOCK");

        paramInfo.setTableNameList(tableNameList);
        state = transferManagementReceiver.deleteCopiedData(paramInfo);
//        assertEquals(MigrateState.DATA_REMOVED_FROM_MAIN, state);
        assertEquals(MigrateState.FAILED, state);

        paramInfo.setShardHash("000000000".getBytes());
        state = transferManagementReceiver.addShardInfo(paramInfo);
        assertEquals(MigrateState.COMPLETED, state);

        log.debug("Migration finished in = {} sec", (System.currentTimeMillis() - start)/1000 );
    }
}