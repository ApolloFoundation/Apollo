/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase.CSV_FILE_EXTENSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.apollocurrency.aplwallet.apl.core.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.account.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.PhasingOnly;
import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStoreServiceImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyBasedFileConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.config.WalletClientProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexOfferMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchServiceImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.LuceneFullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriterImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
import com.apollocurrency.aplwallet.apl.core.tagged.TaggedDataServiceImpl;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferTable;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.slf4j.Logger;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class CsvWriterReaderDerivedTablesTest {
    private static final Logger log = getLogger(CsvWriterReaderDerivedTablesTest.class);

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("csvImportDb").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    private NtpTime time = mock(NtpTime.class);
    private LuceneFullTextSearchEngine ftlEngine = new LuceneFullTextSearchEngine(time, temporaryFolderExtension.newFolder("indexDirPath").toPath());
    private FullTextSearchService ftlService = new FullTextSearchServiceImpl(ftlEngine, Set.of("tagged_data", "currency"), "PUBLIC");
    private KeyStoreService keyStore = new VaultKeyStoreServiceImpl(temporaryFolderExtension.newFolder("keystorePath").toPath(), time);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainImpl.class, DaoConfig.class,
            PropertyProducer.class, TransactionApplier.class, ServiceModeDirProvider.class,
            BlockchainProcessorImpl.class, TrimService.class,
            JdbiHandleFactory.class,
            TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class,
            GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class,
            ReferencedTransactionDaoImpl.class,
            TaggedDataDao.class, DexService.class, DexOfferTable.class, EthereumWalletService.class,
            DexOfferMapper.class, WalletClientProducer.class, PropertyBasedFileConfig.class,
            DataTagDao.class, PhasingPollServiceImpl.class, PhasingPollResultTable.class,
            PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class, PhasingVoteTable.class, PhasingPollTable.class,
            KeyFactoryProducer.class, FeeCalculator.class,
            TaggedDataTimestampDao.class,
            TaggedDataExtendDao.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(time, NtpTime.class))
            .addBeans(MockBean.of(ftlEngine, FullTextSearchEngine.class))
            .addBeans(MockBean.of(ftlService, FullTextSearchService.class))
            .addBeans(MockBean.of(keyStore, KeyStoreService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .build();

    @Inject
    JdbiHandleFactory jdbiHandleFactory;
    @Inject
    private Blockchain blockchain;
    @Inject
    DerivedTablesRegistry registry;
    CsvWriter csvWriter;
    CsvReader csvReader;

    public CsvWriterReaderDerivedTablesTest() throws Exception {}

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
//        registry.getDerivedTables().clear();
    }

    @BeforeEach
    void setUp() {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(chain).getChainId();
        // init several derived tables
        AccountCurrencyTable.getInstance().init();
        PhasingOnly.get(Long.parseUnsignedLong("2728325718715804811"));
        AccountAssetTable.getInstance().init();
        GenesisPublicKeyTable.getInstance().init();
        PublicKeyTable publicKeyTable = new PublicKeyTable(blockchain);
        publicKeyTable.init();
        DGSPurchaseTable purchaseTable = new DGSPurchaseTable();
        purchaseTable.init();
    }

    @Test
    void testExportAndImportData() {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("csvExport").toPath()).when(dirProvider).getDataExportDir();
        // init columns excludes from export
        HashSet<String> excludeColumnNames = new HashSet<>();
        excludeColumnNames.add("DB_ID");
//        excludeColumnNames.add("PUBLIC_KEY");
        excludeColumnNames.add("LATEST");
        // init Cvs reader, writer components
        csvWriter = new CsvWriterImpl(dirProvider.getDataExportDir(), excludeColumnNames, "DB_ID");
        csvWriter.setOptions("fieldDelimiter="); // do not put ""
        csvReader = new CsvReaderImpl(dirProvider.getDataExportDir());
        csvReader.setOptions("fieldDelimiter="); // do not put ""

        Collection<DerivedTableInterface> result = registry.getDerivedTables(); // extract all derived tables

        assertNotNull(result);
        log.debug("Processing [{}] tables", result.size());
//        assertEquals(12, result.size()); // the real number is higher then initial, it's OK !
        int targetHeight = 8000;
        result.forEach(item -> {
            assertNotNull(item);
            log.debug("Table = '{}'", item.toString());
            long minDbValue = 0;
            long maxDbValue = 0;
            int processedCount = 0;
            int totalCount = 0;
            int batchLimit = 1; // used for pagination and partial commit

            // prepare connection + statement
            try (Connection con = extension.getDatabaseManger().getDataSource().getConnection();
                 PreparedStatement pstmt = con.prepareStatement("select * from " + item.toString() + " where db_id > ? and db_id < ? limit ?")) {
                // select Min, Max DbId + rows count
                MinMaxDbId minMaxDbId = item.getMinMaxDbId(targetHeight);
                minDbValue = minMaxDbId.getMinDbId();
                maxDbValue = minMaxDbId.getMaxDbId();
                assertTrue(minMaxDbId.getMaxDbId() >= 0);
                log.debug("Table = {}, Min/Max = {} at height = {}", item.toString(), minMaxDbId, targetHeight);

                // process non empty tables
                if (minMaxDbId.getCount() > 0) {
                    do { // do exporting into csv with pagination
                        processedCount = csvWriter.append(item.toString(),
                                item.getRangeByDbId(con, pstmt, minMaxDbId, batchLimit), minMaxDbId );
                        totalCount += processedCount;
                    } while (processedCount > 0); //keep processing while not found more rows
                    csvWriter.close(); // close CSV file

                    log.debug("Table = {}, exported rows = {}", item.toString(), totalCount);
                    assertEquals(minMaxDbId.getCount(), totalCount);

                    int deletedCount = dropDataByName(minDbValue, maxDbValue, item.toString()); // drop exported data only
                    assertEquals(minMaxDbId.getCount(), deletedCount);

                    int imported = importCsv(item.toString(), batchLimit);
                    log.debug("Table = {}, imported rows = {}", item.toString(), imported);
                    assertEquals(minMaxDbId.getCount(), imported, "incorrect value for " + item.toString());

                }
            } catch (SQLException e) {
                log.error("Exception", e);
            }

        });
        log.debug("Processed Tables = {}", result);
    }

/*
    private int importCsv(String itemName, int batchLimit) {
        int importedCount = 0;
        int columnsCount = 0;
        PreparedStatement preparedInsertStatement = null;
        // open CSV Reader and db connection
        try (ResultSet rs = csvReader.read(itemName + CSV_FILE_EXTENSION, null, null);
             Connection con = extension.getDatabaseManger().getDataSource().getConnection()) {

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            columnsCount = meta.getColumnCount(); // columns count is main
            // create SQL insert statement
            StringBuffer sqlInsert = new StringBuffer(600);
            StringBuffer columnNames = new StringBuffer(200);
            StringBuffer columnsValues = new StringBuffer(200);
            sqlInsert.append("INSERT INTO ").append(itemName.toString()).append(" (");
            for (int i = 0; i < columnsCount; i++) {
                columnNames.append( meta.getColumnLabel(i + 1)).append(",");
                columnsValues.append("?").append(",");
            }
            columnNames.deleteCharAt(columnNames.lastIndexOf(",")); // remove latest tail comma
            columnsValues.deleteCharAt(columnsValues.lastIndexOf(",")); // remove latest tail comma
            sqlInsert.append(columnNames).append(") VALUES").append("(").append(columnsValues).append(")");
            log.debug("SQL = {}", sqlInsert.toString()); // composed insert
            // precompile insert SQL
            preparedInsertStatement = con.prepareStatement(sqlInsert.toString());

            // loop over CSV data reading line by line, column by column
            while (rs.next()) {
                for (int i = 0; i < columnsCount; i++) {
                    Object object = rs.getObject(i + 1);
//                    if (object instanceof String && ((String) object).startsWith("X'")) {
                    if (meta.getColumnType(i + 1) == Types.BINARY || meta.getColumnType(i + 1) == Types.VARBINARY) {
//                        preparedInsertStatement.setBytes(i + 1, ((String)object).getBytes());
//                        preparedInsertStatement.setBytes(i + 1, Convert.parseHexString((String)object));
                        InputStream is = new ByteArrayInputStream( ((String)object).getBytes(StandardCharsets.UTF_8) );
                        preparedInsertStatement.setBinaryStream(i + 1, is);

                    } else {
                        preparedInsertStatement.setObject(i + 1, object);
                    }
                    log.trace("{}: {}\n", meta.getColumnName(i + 1), object);
                }
                log.trace("sql = {}", sqlInsert);
                importedCount += preparedInsertStatement.executeUpdate();
                if (batchLimit % importedCount == 0) {
                    con.commit();
                }
            }
            con.commit(); // final commit
        } catch (SQLException e) {
            log.error("Error on importing data on table = '{}'", itemName, e);
        } finally {
            if (preparedInsertStatement != null) {
                DbUtils.close(preparedInsertStatement);
            }
        }
        return importedCount;
    }
*/

    private int importCsv(String itemName, int batchLimit) {
        int importedCount = 0;
        int columnsCount = 0;
        Statement stm = null;
        // open CSV Reader and db connection
        try (ResultSet rs = csvReader.read(
                itemName, null, null);
             Connection con = extension.getDatabaseManger().getDataSource().getConnection()) {

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            columnsCount = meta.getColumnCount(); // columns count is main
            // create SQL insert statement
            StringBuffer sqlInsert = new StringBuffer(600);
            StringBuffer columnNames = new StringBuffer(200);
            StringBuffer columnsValues = new StringBuffer(200);
            sqlInsert.append("INSERT INTO ").append(itemName).append(" (");
            for (int i = 0; i < columnsCount; i++) {
                columnNames.append( meta.getColumnLabel(i + 1)).append(",");
            }
            columnNames.deleteCharAt(columnNames.lastIndexOf(",")); // remove latest tail comma
            sqlInsert.append(columnNames).append(") VALUES").append(" (");
            log.debug("SQL = {}", sqlInsert.toString()); // composed insert
            // precompile insert SQL
            stm = con.createStatement();

            // loop over CSV data reading line by line, column by column
            while (rs.next()) {
                for (int i = 0; i < columnsCount; i++) {
                    String object = rs.getString(i + 1);
                    log.trace("{}: {}\n", object, rs.getString(i + 1));
                    columnsValues.append(object).append(",");
                }
                columnsValues.deleteCharAt(columnsValues.lastIndexOf(",")); // remove latest tail comma
                StringBuffer sql = new StringBuffer(sqlInsert).append(columnsValues).append(")");
                log.trace("sql = {}", sql);
                importedCount += stm.executeUpdate(sql.toString());
                if (batchLimit % importedCount == 0) {
                    con.commit();
                }
                columnsValues.setLength(0);
            }
            con.commit(); // final commit
        } catch (SQLException e) {
            log.error("Error on importing data on table = '{}'", itemName, e);
        } finally {
            if (stm != null) {
                DbUtils.close(stm);
            }
        }
        return importedCount;
    }

    /**
     * Delete rows in table
     * @param minDbValue min db id
     * @param maxDbValue max db id
     * @param itemName derived table name
     * @return deleted rows quantity
     */
    private int dropDataByName(long minDbValue, long maxDbValue, String itemName) {
        // drop data
        try (Connection con = extension.getDatabaseManger().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("delete from " + itemName + " where db_id > ? AND db_id < ?")) {
            pstmt.setLong(1, minDbValue);
            pstmt.setLong(2, maxDbValue);
            int deleted = pstmt.executeUpdate();
            log.debug("Table = {}, deleted = {} by MIN = {} / MAX = {}", itemName.toString(), deleted, minDbValue, maxDbValue);
            return deleted;
        } catch (SQLException e) {
            log.error("Exception", e);
        }
        return -1;
    }

    @Test
    void incorrectParamsSuppliedToReader() {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("csvExport").toPath()).when(dirProvider).getDataExportDir();

        assertThrows(NullPointerException.class, () -> {
            csvReader = new CsvReaderImpl(null);
        });

        csvReader = new CsvReaderImpl(dirProvider.getDataExportDir());
        csvReader.setOptions("fieldDelimiter="); // do not put ""

        String tableName = "unknown_table_name";
        assertThrows(SQLException.class, () -> {
            ResultSet rs = csvReader.read(tableName + CSV_FILE_EXTENSION, null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            ResultSet rs = csvReader.read(null, null, null);
        });
    }

    @Test
    void incorrectParamsSuppliedToWriter() {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("csvExport").toPath()).when(dirProvider).getDataExportDir();

        assertThrows(NullPointerException.class, () -> {
            csvWriter = new CsvWriterImpl(null, Collections.emptySet(), null);
        });

        csvWriter = new CsvWriterImpl(dirProvider.getDataExportDir(), Collections.emptySet(), null);
        csvWriter.setOptions("fieldDelimiter="); // do not put ""

        String tableName = "unknown_table_name";
        assertThrows(NullPointerException.class, () -> {
            csvWriter.write(tableName + CSV_FILE_EXTENSION, null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            csvWriter.write(tableName, new SimpleResultSet(), null);
        });
    }
}