package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

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
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
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
import javax.inject.Inject;

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
            JdbiHandleFactory.class,
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
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
            .addBeans(MockBean.of(mock(TrimService.class), TrimService.class))
            .addBeans(MockBean.of(time, NtpTime.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .build();

    @Inject
    private AplAppStatus aplAppStatus;
    CsvImporter csvImporter;

    private Set<String> tables = Set.of("account_control_phasing", "phasing_poll", "public_key", "purchase", "shard", "shuffling_data");

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
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null);
        assertNotNull(csvImporter);
        long result = csvImporter.importCsv("unknown_table_file", 10, true);
        assertEquals(-1, result);
    }

    @Test
    void importCsv() throws Exception {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null);
        assertNotNull(csvImporter);

        for (String tableName : tables) {
            long result = csvImporter.importCsv(tableName, 1, true);
            assertTrue(result > 0, "incorrect '" + tableName + "'");
            log.debug("Imported '{}' rows for table '{}'", result, tableName);

            verifyCount(tableName, result);

            List<String> lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
            int numberOfLines = lineInCsv.size();
            assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");
        }
    }

    @Test
    void testImportAccountControlPhasingCsvWithArrayOfLongs() throws Exception {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null);

        String tableName = "account_control_phasing";

        long result = csvImporter.importCsv(tableName, 1, true);
        assertEquals(4, result);
        try (Connection con = extension.getDatabaseManager().getDataSource().getConnection();
             Statement stmt = con.createStatement()) {
            ResultSet countRs = stmt.executeQuery("select count(*) from " + tableName);
            countRs.next();
            assertEquals(4, countRs.getInt(1));
            ResultSet allRs = stmt.executeQuery("select * from " + tableName);
            while (allRs.next()) {
                PhasingOnly phasingOnly = new PhasingOnly(allRs, null); // should not fail
                long[] whitelist = phasingOnly.getPhasingParams().getWhitelist();
                assertNotNull(whitelist);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<String> lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
        int numberOfLines = lineInCsv.size();
        assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");
    }

    @Test
    void testImportShufflingDataCsvWithArrayOfByteArrays() throws Exception {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null);

        String tableName = "shuffling_data";

        long result = csvImporter.importCsv(tableName, 1, true);
        assertEquals(2, result);
        try (Connection con = extension.getDatabaseManager().getDataSource().getConnection();
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
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<String> lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
        int numberOfLines = lineInCsv.size();
        assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");
    }

    @Test
    void testImportGoodsCsvWithArrayOfStrings() throws Exception {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), null);

        String tableName = "goods";
        long result = csvImporter.importCsv(tableName, 1, true);
        assertEquals(14, result);
        try (Connection con = extension.getDatabaseManager().getDataSource().getConnection();
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
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<String> lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
        int numberOfLines = lineInCsv.size();
        assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");

    }

    @Test
    void importBigAccountCsvList() throws Exception {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), aplAppStatus);
        assertNotNull(csvImporter);

        String taskId = aplAppStatus.durableTaskStart("Shard data import", "data import", true);

        String tableName = "account"; // 85000 records is prepared
        long result = csvImporter.importCsvWithDefaultParams(tableName, 10, true, Map.of("height", 100));
        assertTrue(result > 0, "incorrect '" + tableName + "'");
        log.debug("Imported '{}' rows for table '{}'", result, tableName);

        List<String> lineInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(tableName + ".csv"));
        int numberOfLines = lineInCsv.size();
        assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");

        verifyCount(tableName, result);
        DbUtils.inTransaction(extension, (con)-> {
            try (PreparedStatement pstmt = con.prepareStatement("select avg(height) from account")) {
                ResultSet rs = pstmt.executeQuery();
                rs.next();
                assertEquals(rs.getDouble(1), 100.0, 0.01);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        });
        aplAppStatus.durableTaskFinished( taskId, false, "data import finished");
    }

    @Test
    void importShardOnly() throws Exception {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), aplAppStatus);
        assertNotNull(csvImporter);

        String tableName = "shard";
        long result = csvImporter.importCsv(tableName, 10, true);
        assertTrue(result > 0, "incorrect '" + tableName + "'");
        log.debug("Imported '{}' rows for table '{}'", result, tableName);

        // check and read actual number of text lines inside CSV and imported rows number
        List<String> shardCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve("shard.csv"));
        int numberOfLines = shardCsv.size();
        assertEquals(numberOfLines - 1, result, "incorrect lines imported from'" + tableName + "'");

        verifyCount(tableName, result);
        // try explicitly extract two ARRAY columns with Long and Integer values inside
        DbUtils.inTransaction(extension, (con)-> {
            try (PreparedStatement pstmt = con.prepareStatement("select GENERATOR_IDS, BLOCK_TIMEOUTS from " + tableName + " order by shard_id")) {
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
    void testImportWithRowHook() throws Exception {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvImporter = new CsvImporterImpl(resourceFileLoader.getResourcePath(), extension.getDatabaseManager(), aplAppStatus);
        AtomicInteger counter = new AtomicInteger(0);
        long result = csvImporter.importCsvWithRowHook(ShardConstants.PHASING_POLL_TABLE_NAME, 10, true, (row) -> {
            counter.incrementAndGet();
        });
        assertEquals(3, counter.get());
        assertEquals(result, counter.get());
        List<String> phasingPollCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve("phasing_poll.csv"));
        assertEquals(phasingPollCsv.size() - 1, result);
        verifyCount(ShardConstants.PHASING_POLL_TABLE_NAME, 3);
    }

    private void verifyCount(String tableName, long count) {
        DbUtils.inTransaction(extension, (con)-> {
            try (PreparedStatement preparedCount = con.prepareStatement("select count(*) as count from " + tableName)
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
        });
    }


}