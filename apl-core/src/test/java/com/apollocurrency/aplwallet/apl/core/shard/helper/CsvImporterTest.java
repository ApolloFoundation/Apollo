/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockSerializer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.TaggedDataServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaperImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionSerializerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionVersionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.testutil.ResourceFileLoader;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ServiceModeDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

@Slf4j

@Tag("slow")
@QuarkusTest
@Execution(ExecutionMode.CONCURRENT)
class CsvImporterTest extends DbContainerBaseTest {

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbFileProperties(createPath("csvExporterDb").toAbsolutePath().toString()));
    CsvImporter csvImporter;
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    PeersService peersService = mock(PeersService.class);
    GeneratorService generatorService = mock(GeneratorService.class);
    TransactionTestData td = new TransactionTestData();
    BlockSerializer blockSerializer = mock(BlockSerializer.class);
    MemPool memPool = mock(MemPool.class);
    UnconfirmedTransactionProcessingService unconfirmedTransactionProcessingService = mock(UnconfirmedTransactionProcessingService.class);

/*    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        BlockchainImpl.class, DaoConfig.class,
        PropertyProducer.class, TransactionApplier.class, ServiceModeDirProvider.class,
        TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class,
        GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class,
        ReferencedTransactionDaoImpl.class,
        AppendixApplierRegistry.class,
        AppendixValidatorRegistry.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TransactionBuilder.class,
        TaggedDataTable.class,
        DataTagDao.class, PhasingPollServiceImpl.class, PhasingPollResultTable.class,
        PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class, PhasingVoteTable.class, PhasingPollTable.class, PhasingApprovedResultTable.class,
        KeyFactoryProducer.class, FeeCalculator.class,
        TaggedDataTimestampDao.class,
        TaggedDataExtendDao.class,
        FullTextConfigImpl.class,
        DerivedDbTablesRegistryImpl.class,
        AplAppStatus.class, TransactionSerializerImpl.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class,
        ValueParserImpl.class, CsvEscaperImpl.class,
        UnconfirmedTransactionTable.class, AccountService.class, TaskDispatchManager.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(extension.getFullTextSearchService(), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
        .addBeans(MockBean.of(mock(TrimService.class), TrimService.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(mock(AccountService.class), AccountServiceImpl.class, AccountService.class))
        .addBeans(MockBean.of(mock(AccountPublicKeyService.class), AccountPublicKeyServiceImpl.class, AccountPublicKeyService.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(mock(AccountControlPhasingService.class), AccountControlPhasingService.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(ntpTimeConfig.time(), NtpTime.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(peersService, PeersService.class))
        .addBeans(MockBean.of(generatorService, GeneratorService.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(mock(CurrencyService.class), CurrencyService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(TransactionVersionValidator.class), TransactionVersionValidator.class))
        .addBeans(MockBean.of(blockSerializer, BlockSerializer.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(unconfirmedTransactionProcessingService, UnconfirmedTransactionProcessingService.class))
        .addBeans(MockBean.of(memPool, MemPool.class))
        .addBeans(MockBean.of(mock(FullTextSearchUpdater.class), FullTextSearchUpdater.class))
        .build();*/

    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);
    @Inject
    private AplAppStatus aplAppStatus;
    @Inject
    private ValueParser valueParser;
    @Inject
    private CsvEscaper translator;
    private final ObjectMapper mapper = new ObjectMapper();

    private Set<String> tables = Set.of("account_info", "account_control_phasing", "phasing_poll", "public_key", "purchase", "shard", "shuffling_data");

    public CsvImporterTest() throws Exception {
    }

    private static Path createPath(String fileName) {
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
//                    PhasingOnly phasingOnly = new PhasingOnly(allRs, null); // should not fail
                    AccountControlPhasing phasingOnly = new AccountControlPhasing(allRs, null); // should not fail
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
                    String data = allRs.getString("data");// should not fail
                    if (data != null) {
                        String[] array = mapper.readValue(data, new TypeReference<>() {});
                        for (int i = 0; i < array.length; i++) {
                            byte[] bytes = array[i].getBytes();
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
            assertEquals(13, result);
            try (Connection con = dataSource.getConnection();
                 Statement stmt = con.createStatement()) {
                ResultSet countRs = stmt.executeQuery("select count(*) from " + tableName);
                countRs.next();
                assertEquals(13, countRs.getInt(1));
                ResultSet allRs = stmt.executeQuery("select * from " + tableName);
                while (allRs.next()) {
                    String data = allRs.getString("parsed_tags");// should not fail
                    assertNotNull(data);
                    String[] array = mapper.readValue(data, String[].class);
                    assertNotNull(array);
                    for (int i = 0; i < array.length; i++) {
                        String tag = array[i];
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
                assertNotNull(rs.getString(1));
                assertTrue(rs.getString(1).length() > 0);
                assertNotNull(rs.getString(2));
                assertTrue(rs.getString(2).length() > 0);
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