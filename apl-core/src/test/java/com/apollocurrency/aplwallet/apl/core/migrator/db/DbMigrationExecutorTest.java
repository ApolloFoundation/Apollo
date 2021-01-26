/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import org.jboss.weld.junit5.EnableWeld;

@EnableWeld
public class DbMigrationExecutorTest {

/*    @RegisterExtension
    static TemporaryFolderExtension temporaryFolder = new TemporaryFolderExtension();
    @Inject
    JdbiHandleFactory jdbiHandleFactory;
    @Inject
    Jdbi jdbi;
    private LegacyDbLocationsProvider legacyDbLocationsProvider = Mockito.mock(LegacyDbLocationsProvider.class);
    private FullTextSearchService fullTextSearchProvider = Mockito.mock(FullTextSearchService.class);
    private PropertiesHolder propertiesHolder = mockPropertiesHolder();
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    private Path targetDbDir = createTempDir();
    private Path targetDbPath = targetDbDir.resolve(Constants.APPLICATION_DIR_NAME);
    private DbProperties targetDbProperties = DbTestData.getDbFileProperties(targetDbPath.toAbsolutePath().toString());
    TransactionTestData td = new TransactionTestData();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(H2DbInfoExtractor.class, PropertyProducer.class,
        TransactionalDataSource.class, DatabaseManagerImpl.class, TransactionDaoImpl.class, JdbiHandleFactory.class,
        GlobalSyncImpl.class,
        TransactionRowMapper.class,
        TransactionBuilder.class,
        BlockDaoImpl.class, DerivedDbTablesRegistryImpl.class, BlockchainConfig.class,
        FullTextConfigImpl.class)
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(Mockito.mock(Blockchain.class), Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(Mockito.mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(targetDbProperties, DbProperties.class))
        .addBeans(MockBean.of(fullTextSearchProvider, FullTextSearchService.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .build();
    @Inject
    private H2DbInfoExtractor h2DbInfoExtractor;
    private Path pathToDbForMigration;
    private DbManipulator manipulator;
    @Inject
    private DatabaseManager databaseManager;

    private Path createTempDir() {
        try {
            return temporaryFolder.newFolder().toPath();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create dbMigration TARGET dir");
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        this.pathToDbForMigration = temporaryFolder.newFolder().toPath().resolve(
            "migrationDb-1");
        manipulator = new DbManipulator(DbTestData.getDbFileProperties(pathToDbForMigration.toAbsolutePath().toString()));
        manipulator.init();
        manipulator.populate();
        manipulator.shutdown();
    }

    PropertiesHolder mockPropertiesHolder() {
        Properties properties = new Properties();
        properties.put("apl.migrator.db.deleteAfterMigration", true);
        properties.put("apl.batchCommitSize", 100);
        properties.put("apl.dbPassword", "sa");
        properties.put("apl.dbUsername", "sa");
        properties.put("apl.dbParams", "DB_CLOSE_ON_EXIT=FALSE;");
        PropertiesHolder ph = new PropertiesHolder();
        ph.init(properties);
        return ph;
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseManager.shutdown();
    }

    @Test
    public void testDbMigrationWhenNoDbsFound() throws IOException {
        DbMigrationExecutor migrationExecutor = new DbMigrationExecutor(
            propertiesHolder, legacyDbLocationsProvider, h2DbInfoExtractor, databaseManager, fullTextSearchProvider, jdbiHandleFactory);
        Mockito.doReturn(Collections.emptyList()).when(legacyDbLocationsProvider).getDbLocations();
        migrationExecutor.performMigration(targetDbPath);
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        String dbMigrated = optionDAO.get("dbMigrationRequired-0");
        Assertions.assertNotNull(dbMigrated);
        Assertions.assertEquals("false", dbMigrated);
        Assertions.assertTrue(Files.exists(h2DbInfoExtractor.getPath(pathToDbForMigration.toAbsolutePath().toString())));
//        Assertions.assertThrows(TransactionException.class, () -> jdbi.open());
        Handle handle = jdbiHandleFactory.open();
        handle.execute("select 1");
        jdbiHandleFactory.close();
        databaseManager.shutdown();
        int migratedHeight = h2DbInfoExtractor
            .getHeight(targetDbPath.toAbsolutePath().toString());
        Assertions.assertEquals(0, migratedHeight);
    }

    @Test
    public void testDbMigration() throws IOException {
        DbMigrationExecutor migrationExecutor = new DbMigrationExecutor(
            propertiesHolder, legacyDbLocationsProvider, h2DbInfoExtractor, databaseManager, fullTextSearchProvider, jdbiHandleFactory);
        Mockito.doReturn(Arrays.asList(pathToDbForMigration)).when(legacyDbLocationsProvider).getDbLocations();
        migrationExecutor.performMigration(targetDbPath);
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        String dbMigrated = optionDAO.get("dbMigrationRequired-0");
        Assertions.assertNotNull(dbMigrated);
        Assertions.assertEquals("false", dbMigrated);
        Assertions.assertFalse(Files.exists(h2DbInfoExtractor.getPath(pathToDbForMigration.toAbsolutePath().toString())));
//        Assertions.assertThrows(ConnectionException.class, () -> jdbi.open());
        Handle handle = jdbiHandleFactory.open();
        handle.execute("select 1");
        jdbiHandleFactory.close();
        databaseManager.shutdown();
        int migratedHeight = h2DbInfoExtractor.getHeight(targetDbPath.toAbsolutePath().toString());
        BlockTestData btd = new BlockTestData();
        Assertions.assertEquals(btd.LAST_BLOCK.getHeight(), migratedHeight);

    }*/
}
