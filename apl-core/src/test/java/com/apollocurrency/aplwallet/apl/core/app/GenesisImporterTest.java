package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountGuaranteedBalance;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableData;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BalancesPublicKeysTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@EnableWeld
class GenesisImporterTest {

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("genesisImport").toAbsolutePath().toString()));

    @Inject
    PropertiesHolder propertiesHolder;

    @Inject
    AccountService accountService;
    @Inject
    AccountPublicKeyService accountPublicKeyService;
    @Inject
    AccountGuaranteedBalanceTable accountGuaranteedBalanceTable;
    @Inject
    AccountTable accountTable;

    BalancesPublicKeysTestData testData;
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainConfigUpdater blockchainConfigUpdater = mock(BlockchainConfigUpdater.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);
    private AplAppStatus aplAppStatus = mock(AplAppStatus.class);
    private GenesisImporterProducer genesisImporterProducer = mock(GenesisImporterProducer.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            AccountTable.class, AccountGuaranteedBalanceTable.class, PublicKeyTableProducer.class,
            AccountServiceImpl.class, AccountPublicKeyServiceImpl.class,
            FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class, PropertiesHolder.class,
            ShardRecoveryDaoJdbcImpl.class, GenesisImporter.class,
            TransactionDaoImpl.class, BlockchainImpl.class,
            BlockDaoImpl.class, TransactionIndexDao.class, DaoConfig.class, ApplicationJsonFactory.class)
            .addBeans(MockBean.of(mock(TimeService.class), TimeService.class))
            .addBeans(MockBean.of(mock(InMemoryCacheManager.class), InMemoryCacheManager.class))
            .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(blockchainConfigUpdater, BlockchainConfigUpdater.class))
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(extension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
            .addBeans(MockBean.of(extension.getFtl(), FullTextSearchService.class))
            .addBeans(MockBean.of(aplAppStatus, AplAppStatus.class))
            .addBeans(MockBean.of(genesisImporterProducer, GenesisImporterProducer.class))
            .addBeans(MockBean.of(mock(GlobalSync.class), GlobalSync.class, GlobalSyncImpl.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .build();
    private GenesisImporter genesisImporter;

    @BeforeEach
    void setUp() {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(3000000000000000000L).when(config).getMaxBalanceATM();
        doReturn(100L).when(config).getInitialBaseTarget();

        testData = new BalancesPublicKeysTestData();

        propertiesHolder.init(
                getGenesisAccountTotalProperties("230730", "84832")
        );
    }

    @Test
    void newGenesisBlock() {
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        propertiesHolder.init(
                getGenesisAccountTotalProperties("10", "10")
        );
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                propertiesHolder,
                accountService,
                accountPublicKeyService
        );
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
        doReturn("conf/data/genesisParameters-INCORRECT.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                propertiesHolder,
                accountService,
                accountPublicKeyService
        );
        assertThrows(RuntimeException.class, () -> {
            genesisImporter.loadGenesisDataFromResources(); // emulate @PostConstruct
        });
    }

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
        doReturn("conf/data/genesisAccounts-HUGE.json").when(chain).getGenesisLocation();
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                propertiesHolder,
                accountService,
                accountPublicKeyService
        );
        genesisImporter.loadGenesisDataFromResources(); // emulate @PostConstruct

        Block block = genesisImporter.newGenesisBlock();
        //genesisImporter.importGenesisJson(false); // COMMENTED OUT because it adds 10+sec to this test
        assertNotNull(block);
        assertEquals(1739068987193023818L, GenesisImporter.CREATOR_ID);
        assertEquals(1515931200000L, GenesisImporter.EPOCH_BEGINNING);
    }

    @Test
    void savePublicKeysOnly() throws Exception {
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.BALANCE_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                mockedPropertiesHolder,
                accountService,
                accountPublicKeyService
        );
        genesisImporter.loadGenesisDataFromResources(); // emulate @PostConstruct

        genesisImporter.importGenesisJson(false);
        int count = accountPublicKeyService.getPublicKeysCount();
        assertEquals(0, count);
        count = accountPublicKeyService.getGenesisPublicKeysCount();
        assertEquals(19, count);
        Account genesisAccount = accountService.getAccount(genesisImporter.CREATOR_ID);
        assertEquals(-43678392484062L , genesisAccount.getBalanceATM());
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
        doReturn("conf/unknown_path/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                propertiesHolder,
                accountService,
                accountPublicKeyService
        );

        assertThrows(RuntimeException.class, () -> genesisImporter.newGenesisBlock());
    }

    @Test
    void savePublicKeysAndBalances() {
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        TransactionalDataSource dataSource = extension.getDatabaseManager().getDataSource();
        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.BALANCE_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                mockedPropertiesHolder,
                accountService,
                accountPublicKeyService
        );
        dataSource.begin();
        genesisImporter.importGenesisJson(true);
        int count = accountPublicKeyService.getPublicKeysCount();
        assertEquals(0, count);
        count = accountPublicKeyService.getGenesisPublicKeysCount();
        assertEquals(10, count);
        checkImportedPublicKeys(10);
    }

    @Test
    void incorrectTotalBalanceValue() {
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        doReturn(30000000L).when(config).getMaxBalanceATM(); // incorrect value here
        TransactionalDataSource dataSource = extension.getDatabaseManager().getDataSource();
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                propertiesHolder,
                accountService,
                accountPublicKeyService
        );
        assertThrows(RuntimeException.class, () -> genesisImporter.importGenesisJson(false));
    }

    @Test
    void missingBalanceValues() {
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet-MISSING-BALANCES.json").when(chain).getGenesisLocation();
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                propertiesHolder,
                accountService,
                accountPublicKeyService
        );
        assertThrows(RuntimeException.class, () -> genesisImporter.importGenesisJson(false));
    }

    private void checkImportedPublicKeys(int countExpected) {
        List<PublicKey> result = accountPublicKeyService.loadPublicKeyList(0, 10, true);
        int countActual = 0;
        for (PublicKey publicKey : result) {
            String toHexString = Convert.toHexString(publicKey.getPublicKey());
            log.trace("publicKeySet contains key = {} = {}", toHexString, testData.publicKeySet.contains(toHexString));
            assertTrue(testData.publicKeySet.contains(Convert.toHexString(publicKey.getPublicKey())),
                "ERROR, publicKeySet doesn't contain key = "
                    + Convert.toHexString(publicKey.getPublicKey()));
            countActual++;
        }
        assertEquals(countExpected, countActual);
    }

    @Test
    void loadGenesisAccounts() {
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.BALANCE_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                mockedPropertiesHolder,
                accountService,
                accountPublicKeyService
        );
        List<Map.Entry<String, Long>> result = genesisImporter.loadGenesisAccounts();
        assertNotNull(result);
        assertEquals(9, result.size()); // genesis is skipped
    }

    @Test
    void loadGenesisAccountsIncorrectKey() {
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet-MISSING-BALANCES.json").when(chain).getGenesisLocation();
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                weld.select(ApplicationJsonFactory.class).get(),
                propertiesHolder,
                accountService,
                accountPublicKeyService
        );
        assertThrows(RuntimeException.class, () -> genesisImporter.loadGenesisAccounts());
    }

    @Test
    void shouldNotSavePublicKeysBecauseOfIncorrectPublicKeyNumberTotal() throws IOException {
        //GIVEN
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        final DatabaseManager databaseManager = mock(DatabaseManager.class);
        final ApplicationJsonFactory jsonFactory = mock(ApplicationJsonFactory.class);
        final TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        when(databaseManager.getDataSource()).thenReturn(dataSource);
        when(dataSource.isInTransaction()).thenReturn(true);
        final JsonParser jsonParser = mock(JsonParser.class);
        when(jsonFactory.createParser(any(InputStream.class))).thenReturn(jsonParser);
        when(jsonParser.isClosed()).thenReturn(true);
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                databaseManager,
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                jsonFactory,
                propertiesHolder,
                accountService,
                accountPublicKeyService
        );

        //WHEN
        final Executable executable =
                () -> genesisImporter.importGenesisJson(true);

        //THEN
        assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void shouldNotSaveBalancesBecauseOfIncorrectBalanceNumberTotal() throws IOException {
        //GIVEN
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
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
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.BALANCE_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                databaseManager,
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                jsonFactory,
                mockedPropertiesHolder,
                accountService,
                accountPublicKeyService
        );

        //WHEN
        final Executable executable =
                () -> genesisImporter.importGenesisJson(false);

        //THEN
        assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void shouldNotLoadGenesisAccountsBecauseOfIncorrectBalanceNumberTotal() throws IOException {
        //GIVEN
        doReturn("conf/data/genesisParameters.json").when(genesisImporterProducer).genesisParametersLocation();
        doReturn("conf/data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        final ApplicationJsonFactory jsonFactory = mock(ApplicationJsonFactory.class);
        final JsonParser jsonParser = mock(JsonParser.class);
        when(jsonFactory.createParser(any(InputStream.class))).thenReturn(jsonParser);
        when(jsonParser.nextToken()).thenReturn(JsonToken.END_OBJECT);
        final PropertiesHolder mockedPropertiesHolder = mock(PropertiesHolder.class);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        when(mockedPropertiesHolder.getIntProperty(GenesisImporter.BALANCE_NUMBER_TOTAL_PROPERTY_NAME))
                .thenReturn(10);
        genesisImporter = new GenesisImporter(
                blockchainConfig,
                blockchainConfigUpdater,
                extension.getDatabaseManager(),
                aplAppStatus,
                genesisImporterProducer,
                accountGuaranteedBalanceTable,
                accountTable,
                jsonFactory,
                mockedPropertiesHolder,
                accountService,
                accountPublicKeyService
        );

        //WHEN
        final Executable executable = () -> genesisImporter.loadGenesisAccounts();

        //THEN
        assertThrows(IllegalStateException.class, executable);
    }

    private Properties getGenesisAccountTotalProperties(
            final String publicKeyNumberTotal,
            final String balanceNumberTotal
    ) {
        Properties properties = new Properties();
        properties.put(GenesisImporter.PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME, publicKeyNumberTotal);
        properties.put(GenesisImporter.BALANCE_NUMBER_TOTAL_PROPERTY_NAME, balanceNumberTotal);
        return properties;
    }

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}