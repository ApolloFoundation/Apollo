package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.account.PhasingOnly;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.app.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaperImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.core.tagged.TaggedDataServiceImpl;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.testutil.ResourceFileLoader;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ServiceModeDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class CsvImporterTest {
    private static final Logger log = getLogger(CsvImporterTest.class);

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("csvExporterDb").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    private NtpTime time = mock(NtpTime.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainImpl.class, DaoConfig.class,
            PropertyProducer.class, TransactionApplier.class, ServiceModeDirProvider.class,
            TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class,
            GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class,
            ReferencedTransactionDaoImpl.class,
            TaggedDataDao.class,
            DataTagDao.class, PhasingPollServiceImpl.class, PhasingPollResultTable.class,
            PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class, PhasingVoteTable.class, PhasingPollTable.class, PhasingApprovedResultTable.class,
            KeyFactoryProducer.class, FeeCalculator.class,
            TaggedDataTimestampDao.class,
            TaggedDataExtendDao.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            AplAppStatus.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            ValueParserImpl.class, CsvEscaperImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
            .addBeans(MockBean.of(mock(TrimService.class), TrimService.class))
            .addBeans(MockBean.of(time, NtpTime.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
            .build();

    @Inject
    private AplAppStatus aplAppStatus;
    @Inject
    private ValueParser valueParser;
    CsvImporter csvImporter;
    @Inject
    private CsvEscaper translator;

    private Set<String> tables = Set.of( "account_info", "account_control_phasing", "phasing_poll", "public_key", "purchase", "shard", "shuffling_data");

    public CsvImporterTest() throws Exception {}

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @BeforeEach
    void setUp() {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(chain).getChainId();
    }

    @Test
    void notFoundFile() throws Exception {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null, valueParser, translator);
        assertNotNull(csvImporter);
        long result = csvImporter.importCsv("unknown_table_file", 10, true);
        assertEquals(-1, result);
    }

    @Test
    void importCsv() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (conOuter) -> {
            csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null, valueParser, translator);
            assertNotNull(csvImporter);

            for (String tableName : tables) {
                long result = 0;
                try {
                    result = csvImporter.importCsv(tableName, 1, true);
                    dataSource.commit(false);
                } catch (Exception e) {
                    log.error("Import error " + tableName, e);
                    throw new RuntimeException(e);
                }
                assertTrue(result > 0, "incorrect '" + tableName + "'");
                log.debug("Imported '{}' rows for table '{}'", result, tableName);

                verifyCount(dataSource, tableName, result);

                List<String> lineInCsv = null;
                try {
                    lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
                } catch (IOException e) {
                    log.error("Load all lines error", e);
                }
                int numberOfLines = lineInCsv.size();
                assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");
            }
        });
    }

    @Test
    void testImportAccountControlPhasingCsvWithArrayOfLongs() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (conOuter) -> {
            csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), databaseManager, null, valueParser, translator);

            String tableName = "account_control_phasing";
            long result = 0;
            try {
                result = csvImporter.importCsv(tableName, 1, true);
                dataSource.commit(false);
            } catch (Exception e) {
                log.error("Import error " + tableName, e);
                throw new RuntimeException(e);
            }
            assertEquals(4, result);

            try (Connection con = dataSource.getConnection();
                 Statement stmt = con.createStatement()) {
                ResultSet countRs = con.createStatement().executeQuery("select count(*) from " + tableName);
                countRs.next();
                assertEquals(4, countRs.getInt(1));
                ResultSet allRs = stmt.executeQuery("select * from " + tableName);
                while (allRs.next()) {
                    PhasingOnly phasingOnly = new PhasingOnly(allRs, null); // should not fail
                    long[] whitelist = phasingOnly.getPhasingParams().getWhitelist();
                    assertNotNull(whitelist);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<String> lineInCsv = null;
            try {
                lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
            } catch (IOException e) {
                log.error("Load all lines error", e);
            }
            int numberOfLines = lineInCsv.size();
            assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");
        });
    }

    @Test
    void testImportShufflingDataCsvWithArrayOfByteArrays() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (conOuter) -> {
            csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null, valueParser, translator);

            String tableName = "shuffling_data";

            long result = 0;
            try {
                result = csvImporter.importCsv(tableName, 1, true);
                dataSource.commit(false);
            } catch (Exception e) {
                log.error("Import error " + tableName, e);
                throw new RuntimeException(e);
            }
            assertEquals(2, result);
            try (Connection con = dataSource.getConnection();
                 Statement stmt = con.createStatement()) {
                ResultSet countRs = stmt.executeQuery("select count(*) from " + tableName);
                countRs.next();
                assertEquals(2, countRs.getInt(1));
                ResultSet allRs = stmt.executeQuery("select * from " + tableName);
                while (allRs.next()) {
                    Array data = allRs.getArray("data");// should not fail
                    if (data != null) {
                        Object[] array = (Object[]) data.getArray();
                        for (int i = 0; i < array.length; i++) {
                            byte[] bytes = (byte[]) array[i];
                            assertNotNull(bytes);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<String> lineInCsv = null;
            try {
                lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
            } catch (IOException e) {
                log.error("Load all lines error", e);
            }
            int numberOfLines = lineInCsv.size();
            assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");
        });
    }

    @Test
    void testImportGoodsCsvWithArrayOfStrings() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (conOuter) -> {
            csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null, valueParser, translator);

            String tableName = "goods";
            long result = 0;
            try {
                result = csvImporter.importCsv(tableName, 1, true);
                dataSource.commit(false);
            } catch (Exception e) {
                log.error("Import error " + tableName, e);
                throw new RuntimeException(e);
            }
            assertEquals(14, result);
            try (Connection con = dataSource.getConnection();
                 Statement stmt = con.createStatement()) {
                ResultSet countRs = stmt.executeQuery("select count(*) from " + tableName);
                countRs.next();
                assertEquals(14, countRs.getInt(1));
                ResultSet allRs = stmt.executeQuery("select * from " + tableName);
                while (allRs.next()) {
                    Array data = allRs.getArray("parsed_tags");// should not fail
                    assertNotNull(data);
                    Object[] array = (Object[]) data.getArray();
                    assertNotNull(array);
                    for (int i = 0; i < array.length; i++) {
                        String tag = (String) array[i];
                        assertNotNull(tag);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<String> lineInCsv = null;
            try {
                lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
            } catch (IOException e) {
                log.error("Load all lines error", e);
            }
            int numberOfLines = lineInCsv.size();
            assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");
        });
    }

    @Test
    void importBigAccountCsvList() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (conOuter) -> {
            csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), aplAppStatus, valueParser, translator);
            assertNotNull(csvImporter);

            String taskId = aplAppStatus.durableTaskStart("Shard data import", "data import", true);

            String tableName = "account"; // 85000 records is prepared
            long result = 0;
            try {
                result = csvImporter.importCsvWithDefaultParams(tableName, 10, true, Map.of("height", 100));
                dataSource.commit(false);
            } catch (Exception e) {
                log.error("Import error " + tableName, e);
                throw new RuntimeException(e);
            }
            assertTrue(result > 0, "incorrect '" + tableName + "'");
            log.debug("Imported '{}' rows for table '{}'", result, tableName);

            List<String> lineInCsv = null;
            try {
                lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
            } catch (IOException e) {
                log.error("Load all lines error", e);
            }
            int numberOfLines = lineInCsv.size();
            assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");

            verifyCount(dataSource, tableName, result);
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("select avg(height) from account")) {
                ResultSet rs = pstmt.executeQuery();
                rs.next();
                assertEquals(rs.getDouble(1), 100.0, 0.01);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }

            aplAppStatus.durableTaskFinished(taskId, false, "data import finished");
        });
    }

    @Test
    void importShardOnly() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (conOuter) -> {

            csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), aplAppStatus, valueParser, translator);
            assertNotNull(csvImporter);

            String tableName = "shard";
            long result = 0;
            try {
                result = csvImporter.importCsv(tableName, 10, true);
                dataSource.commit(false);
            } catch (Exception e) {
                log.error("Import error " + tableName, e);
                throw new RuntimeException(e);
            }
            assertTrue(result > 0, "incorrect '" + tableName + "'");
            log.debug("Imported '{}' rows for table '{}'", result, tableName);

            // check and read actual number of text lines inside CSV and imported rows number
            List<String> shardCsv = null;
            try {
                shardCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve("shard.csv"));
            } catch (IOException e) {
                log.error("Load all lines error", e);
            }
            int numberOfLines = shardCsv.size();
            assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");

            verifyCount(dataSource, tableName, result);
            // try explicitly extract two ARRAY columns with Long and Integer values inside
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("select GENERATOR_IDS, BLOCK_TIMEOUTS from " + tableName + " order by shard_id")) {
                ResultSet rs = pstmt.executeQuery();
                rs.next();
                assertNotNull(rs.getArray(1).getArray());
                assertNotNull(rs.getArray(2).getArray());
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        });
    }

    @Test
    void testImportWithRowHook() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (conOuter) -> {
            csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), aplAppStatus, valueParser, translator);
            AtomicInteger counter = new AtomicInteger(0);
            long result = 0;
            try {
                result = csvImporter.importCsvWithRowHook(ShardConstants.PHASING_POLL_TABLE_NAME, 10, true, (row) -> {
                    counter.incrementAndGet();
                });
                dataSource.commit(false);
            } catch (Exception e) {
                log.error("Import error " + ShardConstants.PHASING_POLL_TABLE_NAME, e);
                throw new RuntimeException(e);
            }
            assertEquals(3, counter.get());
            assertEquals(result, counter.get());
            List<String> phasingPollCsv = null;
            try {
                phasingPollCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve("phasing_poll.csv"));
            } catch (IOException e) {
                log.error("Load all lines error", e);
                throw new RuntimeException(e);
            }
            assertEquals(phasingPollCsv.size() - 1, result);
            verifyCount(dataSource, ShardConstants.PHASING_POLL_TABLE_NAME, 3);
        });
    }

    @Test
    void importDexContracts() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (conOuter) -> {
            csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), aplAppStatus, valueParser, translator);
            assertNotNull(csvImporter);

            String tableName = "dex_contract"; // 65 records is prepared
            long result = 0;
            try {
                result = csvImporter.importCsv(tableName, 10, true);
                dataSource.commit(false);
            } catch (Exception e) {
                log.error("Import error " + tableName, e);
                throw new RuntimeException(e);
            }
            assertTrue(result > 0, "incorrect '" + tableName + "'");
            log.debug("Imported '{}' rows for table '{}'", result, tableName);

            List<String> lineInCsv = null;
            try {
                lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
            } catch (IOException e) {
                log.error("Load all lines error", e);
            }
            int numberOfLines = lineInCsv.size();
            assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");
        });
    }

    private void verifyCount(TransactionalDataSource dataSource, String tableName, long count) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement preparedCount = con.prepareStatement("select count(*) as count from " + tableName)
        ) {
            long result = -1;
            ResultSet rs = preparedCount.executeQuery();
            if (rs.next()) {
                result = rs.getLong("count");
            }
            assertEquals(count, result);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }


}