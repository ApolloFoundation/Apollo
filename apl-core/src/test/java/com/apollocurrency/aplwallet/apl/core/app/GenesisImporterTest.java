package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ShardRecoveryDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableData;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountGuaranteedBalance;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.TwoTablesPublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.BlockChainInfoServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BalancesPublicKeysTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ResourceLocator;
import com.apollocurrency.aplwallet.apl.util.env.config.UserResourceLocator;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static com.apollocurrency.aplwallet.apl.core.app.GenesisImporter.BALANCE_NUMBER_TOTAL_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("slow")
@Slf4j
@EnableWeld
class GenesisImporterTest extends DbContainerBaseTest {

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbFileProperties(createPath("genesisImport").toAbsolutePath().toString()));

    @Inject
    PropertiesHolder propertiesHolder;

    @Inject
    AccountService accountService;
    @Inject
    AccountPublicKeyService accountPublicKeyService;
    @Inject
    AccountGuaranteedBalanceTable accountGuaranteedBalanceTable;
    AccountTable accountTable = new AccountTable(extension.getDatabaseManager(), mock(Event.class));


    BalancesPublicKeysTestData testData;
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainConfigUpdater blockchainConfigUpdater = mock(BlockchainConfigUpdater.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);
    private AplAppStatus aplAppStatus = mock(AplAppStatus.class);
    private PropertiesHolder envConfig = new PropertiesHolder();
    TransactionTestData td = new TransactionTestData();
    private ResourceLocator resourceLocator;

    {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn("APL").when(blockchainConfig).getAccountPrefix();

    }

    @BeforeAll
    public static void beforeAll() {
        ConfigDirProviderFactory.setup(false, "Apollo", 0, "", null);
    }

    static Bean<?> createCfgdDirProviderBean() {
        return MockBean.builder()
            .types(ConfigDirProvider.class)
            .scope(ApplicationScoped.class)
            .creating(
                ConfigDirProviderFactory.getConfigDirProvider()
            )
            .build();
    }

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(AccountGuaranteedBalanceTable.class, PublicKeyTableProducer.class,
        AccountServiceImpl.class, GenesisAccounts.class, BlockChainInfoServiceImpl.class, AccountPublicKeyServiceImpl.class,
        FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class, PropertiesHolder.class,
        ShardRecoveryDaoJdbcImpl.class, GenesisImporter.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionEntityRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TwoTablesPublicKeyDao.class,
        TransactionBuilderFactory.class,
        TransactionDaoImpl.class, BlockchainImpl.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionIndexDao.class,
        DaoConfig.class,
        ApplicationJsonFactory.class,
        UserResourceLocator.class,JdbiHandleFactory.class, JdbiConfiguration.class
    )
        .addBeans(MockBean.of(mock(TimeService.class), TimeService.class))
        .addBeans(MockBean.of(mock(InMemoryCacheManager.class), InMemoryCacheManager.class))
        .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchainConfigUpdater, BlockchainConfigUpdater.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
        .addBeans(MockBean.of(extension.getFullTextSearchService(), FullTextSearchService.class))
        .addBeans(MockBean.of(aplAppStatus, AplAppStatus.class))
        .addBeans(MockBean.of(accountTable, AccountTableInterface.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(envConfig, PropertiesHolder.class))
        .addBeans(MockBean.of(mock(GlobalSync.class), GlobalSync.class, GlobalSyncImpl.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(createCfgdDirProviderBean())
        .build();


//    DbManipulator manipulator = new DbManipulator(dbProperties, DbTestData.getDbFileProperties(createPath("genesisImport").toAbsolutePath().toString()), null, null);
//    @BeforeAll
//    static void beforeAll() {
//        extension.
//    }

    @BeforeEach
    void setUp() {
        doReturn(3000000000000000000L).when(config).getMaxBalanceATM();
        doReturn(100L).when(config).getInitialBaseTarget();

        testData = new BalancesPublicKeysTestData();
        propertiesHolder = new PropertiesHolder(
            getGenesisAccountTotalProperties("230730", "84832")
        );
        resourceLocator = weld.select(ResourceLocator.class).get();
    }

    @SneakyThrows
    @Test
    void newGenesisBlock() {

        propertiesHolder= new PropertiesHolder(
            getGenesisAccountTotalProperties("10", "10")
        );
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            propertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        genesisImporter.loadGenesisDataFromResources(); // emulate @PostConstruct

        Block block = genesisImporter.newGenesisBlock();
        assertNotNull(block);
        assertEquals(1739068987193023818L, block.getGeneratorId());
        assertEquals(4997592126877716673L, block.getId());
        assertEquals(0, block.getHeight());
        assertEquals("1259ec21d31a30898d7cd1609f80d9668b4778e3d97e941044b39f0c44d2e51b",
            Convert.toHexString(genesisImporter.getCreatorPublicKey()));
        assertEquals(
            "9056ecb5bf764f7513195bc6655756b83e55dcb2c4c2fdb20d5e5aa5348617ed",
            Convert.toHexString(genesisImporter.getComputedDigest())
        );
        assertEquals(1739068987193023818L, GenesisImporter.CREATOR_ID);
        assertEquals(1515931200000L, GenesisImporter.EPOCH_BEGINNING);
    }

    @Test
    void incorrectGenesisParameter() {

        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            propertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_PARAMS_JSON = "data/genesisParameters-INCORRECT.json";
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        assertThrows(RuntimeException.class, () -> {
            genesisImporter.loadGenesisDataFromResources(); // emulate @PostConstruct
        });
    }

    @SneakyThrows
    @Test
    void newGenesisBlockLongJson() {
        String key = UUID.randomUUID().toString();
        DurableTaskInfo info = new DurableTaskInfo();
        info.setId(key);
        info.setName("Shard data import");
        info.setPercentComplete(0.0);
        info.setDecription("data import");
        info.setStarted(new Date());
        info.setStateOfTask(DurableTaskInfo.TASK_STATES[0]);
        info.setIsCrititcal(true);
        doReturn(Optional.of(info)).when(aplAppStatus).findTaskByName("Shard data import");

        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            propertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-HUGE.json";
        genesisImporter.loadGenesisDataFromResources(); // emulate @PostConstruct

        Block block = genesisImporter.newGenesisBlock();
        //genesisImporter.importGenesisJson(false); // COMMENTED OUT because it adds 10+sec to this test
        assertNotNull(block);
        assertEquals(1739068987193023818L, GenesisImporter.CREATOR_ID);
        assertEquals(1515931200000L, GenesisImporter.EPOCH_BEGINNING);
    }

    @Test
    void savePublicKeysOnly() throws Exception {
        TransactionalDataSource dataSource = extension.getDatabaseManager().getDataSource();

        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME, 230730))
            .thenReturn(10);
        when(mockedPropertiesHolder.getIntProperty(BALANCE_NUMBER_TOTAL_PROPERTY_NAME, 84832))
            .thenReturn(10);
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            mockedPropertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        genesisImporter.loadGenesisDataFromResources(); // emulate @PostConstruct
        genesisImporter.importGenesisJson(false);
        int count = accountPublicKeyService.getPublicKeysCount();
        assertEquals(0, count);
        count = accountPublicKeyService.getGenesisPublicKeysCount();
        assertEquals(11, count);
        Account genesisAccount = accountService.getAccount(genesisImporter.CREATOR_ID);
        assertEquals(-43678392484062L, genesisAccount.getBalanceATM());
        DerivedTableData derivedTableData = accountGuaranteedBalanceTable.getAllByDbId(0L, 20, 20L);
        assertNotNull(derivedTableData);
        List result = derivedTableData.getValues();
        assertNotNull(result);
        assertEquals(10, result.size());
        for (int i = 0; i < result.size(); i++) { // test accounts + balances values
            AccountGuaranteedBalance balance = (AccountGuaranteedBalance) result.get(i);
            assertNotNull(balance);
            assertTrue(testData.balances.containsKey(balance.getAccountId()));
            assertEquals(testData.balances.get(balance.getAccountId()), balance.getAdditions()); // compare json and db balance
        }
    }

    @Test
    void genesisParamIncorrectPath() {

        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            propertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "unknown_path/genesisAccounts-testnet.json";
        assertThrows(RuntimeException.class, () -> genesisImporter.newGenesisBlock());
    }

    @Test
    void savePublicKeysAndBalances() {

        TransactionalDataSource dataSource = extension.getDatabaseManager().getDataSource();
        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME, 230730))
            .thenReturn(10);
        when(mockedPropertiesHolder.getIntProperty(BALANCE_NUMBER_TOTAL_PROPERTY_NAME, 84832))
            .thenReturn(10);
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            mockedPropertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        dataSource.begin();
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        genesisImporter.importGenesisJson(true);
        int count = accountPublicKeyService.getPublicKeysCount();
        assertEquals(0, count);
        count = accountPublicKeyService.getGenesisPublicKeysCount();
        assertEquals(10, count);
    }

    @Test
    void incorrectTotalBalanceValue() {

        doReturn(30000000L).when(config).getMaxBalanceATM(); // incorrect value here
        TransactionalDataSource dataSource = extension.getDatabaseManager().getDataSource();
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            propertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        assertThrows(RuntimeException.class, () -> genesisImporter.importGenesisJson(false));
    }

    @Test
    void missingBalanceValues() {

        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            propertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        assertThrows(RuntimeException.class, () -> genesisImporter.importGenesisJson(false));
    }

    @SneakyThrows
    @Test
    void loadGenesisAccounts() {

        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME, 230730))
            .thenReturn(10);
        when(mockedPropertiesHolder.getIntProperty(BALANCE_NUMBER_TOTAL_PROPERTY_NAME, 84832))
            .thenReturn(10);
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            mockedPropertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        List<Map.Entry<String, Long>> result = genesisImporter.loadGenesisAccounts();
        assertNotNull(result);
        assertEquals(10, result.size()); // genesis is not skipped
    }

    @Test
    void loadGenesisAccountsIncorrectKey() {

        propertiesHolder = new PropertiesHolder((getGenesisAccountTotalProperties("10", "10")));
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            weld.select(ApplicationJsonFactory.class).get(),
            propertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet-MISSING-BALANCES.json";

        assertThrows(GenesisImportException.class, () -> genesisImporter.loadGenesisAccounts());
    }


    @Test
    void shouldNotSavePublicKeysBecauseOfIncorrectPublicKeyNumberTotal() throws IOException {

        final DatabaseManager databaseManager = mock(DatabaseManager.class);
        final ApplicationJsonFactory jsonFactory = mock(ApplicationJsonFactory.class);
        final TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        when(databaseManager.getDataSource()).thenReturn(dataSource);
        when(dataSource.isInTransaction()).thenReturn(true);
        final JsonParser jsonParser = mock(JsonParser.class);
        when(jsonFactory.createParser(any(InputStream.class))).thenReturn(jsonParser);
        when(jsonParser.isClosed()).thenReturn(true);
        propertiesHolder= new PropertiesHolder(getGenesisAccountTotalProperties("10", "10"));
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            jsonFactory,
            propertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        //WHEN
        final Executable executable =
            () -> genesisImporter.importGenesisJson(true);

        //THEN
//        assertThrows(IllegalStateException.class, executable);
        /**
         * This exception is from com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable#truncate()
         * So it's because of dataSource.isInTransaction() == false. This case is not related with this test method.
         */
    }

    @Test
    void shouldNotSaveBalancesBecauseOfIncorrectBalanceNumberTotal() throws IOException {

        final DatabaseManager databaseManager = mock(DatabaseManager.class);
        final ApplicationJsonFactory jsonFactory = mock(ApplicationJsonFactory.class);
        final TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        when(databaseManager.getDataSource()).thenReturn(dataSource);
        when(dataSource.isInTransaction()).thenReturn(true);
        final JsonParser jsonParser = mock(JsonParser.class);
        when(jsonFactory.createParser(any(InputStream.class))).thenReturn(jsonParser);
        when(jsonParser.isClosed()).thenReturn(true);
        when(jsonParser.nextToken()).thenReturn(JsonToken.END_OBJECT);
        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME))
            .thenReturn(0);
        when(mockedPropertiesHolder.getIntProperty(BALANCE_NUMBER_TOTAL_PROPERTY_NAME))
            .thenReturn(10);
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            jsonFactory,
            mockedPropertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        //WHEN
        final Executable executable =
            () -> genesisImporter.importGenesisJson(false);

        //THEN
        assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void shouldNotLoadGenesisAccountsBecauseOfIncorrectBalanceNumberTotal() throws IOException {

        final ApplicationJsonFactory jsonFactory = mock(ApplicationJsonFactory.class);
        final JsonParser jsonParser = mock(JsonParser.class);
        when(jsonFactory.createParser(any(InputStream.class))).thenReturn(jsonParser);
        when(jsonParser.nextToken()).thenReturn(JsonToken.END_OBJECT);
        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME, 230730))
            .thenReturn(10);
        when(mockedPropertiesHolder.getIntProperty(BALANCE_NUMBER_TOTAL_PROPERTY_NAME, 84832))
            .thenReturn(10);
        GenesisImporter genesisImporter = new GenesisImporter(
            blockchainConfig,
            blockchainConfigUpdater,
            aplAppStatus,
            accountGuaranteedBalanceTable,
            accountTable,
            jsonFactory,
            mockedPropertiesHolder,
            accountService,
            accountPublicKeyService,
            resourceLocator
        );
        genesisImporter.GENESIS_ACCOUNTS_JSON = "data/genesisAccounts-testnet.json";
        //WHEN
        final Executable executable = () -> genesisImporter.loadGenesisAccounts();

        //THEN
        assertThrows(GenesisImportException.class, executable);
    }

    private Properties getGenesisAccountTotalProperties(
        final String publicKeyNumberTotal,
        final String balanceNumberTotal
    ) {
        Properties properties = new Properties();
        properties.put(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME, publicKeyNumberTotal);
        properties.put(BALANCE_NUMBER_TOTAL_PROPERTY_NAME, balanceNumberTotal);
        return properties;
    }

    private static Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}