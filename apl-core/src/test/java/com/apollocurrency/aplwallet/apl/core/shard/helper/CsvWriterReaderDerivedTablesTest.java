/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.account.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.PhasingOnly;
import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.app.TimeServiceImpl;
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
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaperImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriterImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.core.tagged.TaggedDataServiceImpl;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ServiceModeDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.google.common.base.Throwables;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase.CSV_FILE_EXTENSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class CsvWriterReaderDerivedTablesTest {
    private static final Logger log = getLogger(CsvWriterReaderDerivedTablesTest.class);

    @RegisterExtension
    DbExtension extension = new DbExtension(Map.of("currency", List.of("code","name", "description"), "tagged_data", List.of("name","description","tags")));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    private NtpTime time = mock(NtpTime.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);
    private KeyStoreService keyStore = new VaultKeyStoreServiceImpl(temporaryFolderExtension.newFolder("keystorePath").toPath(), time);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainImpl.class, DaoConfig.class,
            PropertyProducer.class, TransactionApplier.class, ServiceModeDirProvider.class,
            TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class,
            GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class,
            ReferencedTransactionDaoImpl.class,
            TaggedDataDao.class, PropertyBasedFileConfig.class,
            DGSGoodsTable.class,
            DataTagDao.class,
            KeyFactoryProducer.class, FeeCalculator.class,
            TaggedDataTimestampDao.class,
            TaggedDataExtendDao.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            DirProvider.class,
            AplAppStatus.class,
            PhasingPollResultTable.class,
            PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class,
            PhasingVoteTable.class, PhasingPollTable.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            CsvEscaperImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(TrimService.class), TrimService.class))
            .addBeans(MockBean.of(time, NtpTime.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
            .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(keyStore, KeyStoreService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
            .build();

    @Inject
    private Blockchain blockchain;
    @Inject
    DerivedTablesRegistry registry;
    @Inject
    private CsvEscaper translator;

    public CsvWriterReaderDerivedTablesTest() throws Exception {}

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @BeforeEach
    void setUp() {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(chain).getChainId();
        // init several derived tables
        AccountCurrencyTable.getInstance().init();
        PhasingOnly.get(Long.parseLong("-8446384352342482748"));
        AccountAssetTable.getInstance().init();
        GenesisPublicKeyTable genesisPublicKeyTable = new GenesisPublicKeyTable(blockchain);
        PublicKeyTable publicKeyTable = new PublicKeyTable(blockchain);
        publicKeyTable.init();
        DGSPurchaseTable purchaseTable = new DGSPurchaseTable();
        purchaseTable.init();
        DexContractTable dexContractTable = new DexContractTable();
        registry.registerDerivedTable(dexContractTable);
        DexOrderTable dexOrderTable = new DexOrderTable();
        registry.registerDerivedTable(dexOrderTable);
    }

    @DisplayName("Gather all derived tables, export data up to height = 8000," +
            " delete rows up to height = 8000, import data back into db table")
    @Test
    void testExportAndImportData() {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("csvExport").toPath()).when(dirProvider).getDataExportDir();
        // init columns excludes from export
        Set<String> excludeColumnNames = Set.of("DB_ID", "LATEST");
        // init Cvs reader, writer components

        Collection<DerivedTableInterface> result = registry.getDerivedTables(); // extract all derived tables

        assertNotNull(result);
        log.debug("Processing [{}] tables", result.size());
//        assertEquals(12, result.size()); // the real number is higher then initial, it's OK !
        int targetHeight = Integer.MAX_VALUE;
        result.forEach(item -> {
            assertNotNull(item);
            log.debug("Table = '{}'", item.toString());
            long minDbValue = 0;
            long maxDbValue = 0;
            int processedCount = 0;
            int totalCount = 0;
            int batchLimit = 1; // used for pagination and partial commit

            // prepare connection + statement + writer
            try (Connection con = extension.getDatabaseManager().getDataSource().getConnection();
                 PreparedStatement pstmt = con.prepareStatement("select * from " + item.toString() + " where db_id BETWEEN ? and  ? limit ?");
                 CsvWriter csvWriter = new CsvWriterImpl(dirProvider.getDataExportDir(), excludeColumnNames, translator);
                 ) {
                csvWriter.setOptions("fieldDelimiter="); // do not put ""
                // select Min, Max DbId + rows count
                MinMaxValue minMaxValue = item.getMinMaxValue(targetHeight);
                minDbValue = minMaxValue.getMin();
                maxDbValue = minMaxValue.getMax();
                assertTrue(minMaxValue.getMax() >= 0);
                log.debug("Table = {}, Min/Max = {} at height = {}", item.toString(), minMaxValue, targetHeight);

                // process non empty tables
                if (minMaxValue.getCount() > 0) {
                    do { // do exporting into csv with pagination
                        CsvExportData csvExportData = csvWriter.append(item.toString(),
                                item.getRangeByDbId(con, pstmt, minMaxValue, batchLimit));

                        processedCount = csvExportData.getProcessCount();
                        if (processedCount > 0) {
                            minMaxValue.setMin((Long) csvExportData.getLastRow().get("DB_ID") + 1);
                        }
                        totalCount += processedCount;
                    } while (processedCount > 0); //keep processing while not found more rows

                    log.debug("Table = {}, exported rows = {}", item.toString(), totalCount);
                    assertEquals(minMaxValue.getCount(), totalCount);

                    int deletedCount = dropDataByName(minDbValue, maxDbValue, item.toString()); // drop exported data only
                    assertEquals(minMaxValue.getCount(), deletedCount);

                    int imported = importCsv(item.toString(), batchLimit, dirProvider.getDataExportDir());
                    log.debug("Table = {}, imported rows = {}", item.toString(), imported);
                    assertEquals(minMaxValue.getCount(), imported, "incorrect value for '" + item.toString() + "'");

                }
            } catch (Exception e) {
                log.error("Exception", e);
                Throwables.throwIfUnchecked(e);
                throw new RuntimeException(e);
            }

        });
        log.debug("Processed Tables = {}", result);
    }

    /**
     * Example for  real implementation importing data
     *
     * @param itemName table name
     * @param batchLimit rows in batch before commit
     * @return processed rows number
     * @throws Exception
     */
    private int importCsv(String itemName, int batchLimit, Path dataExportDir) throws Exception {
        int importedCount = 0;
        int columnsCount = 0;
        PreparedStatement preparedInsertStatement = null;
        ValueParser parser = new ValueParserImpl(translator);

        // open CSV Reader and db connection
        try (CsvReader csvReader = new CsvReaderImpl(dataExportDir, translator);
             ResultSet rs = csvReader.read(itemName + CSV_FILE_EXTENSION, null, null);
             Connection con = extension.getDatabaseManager().getDataSource().getConnection()
        ) {
            csvReader.setOptions("fieldDelimiter="); // do not put ""

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            columnsCount = meta.getColumnCount(); // columns count is main
            // create SQL insert statement
            StringBuffer sqlInsert = new StringBuffer(600);
            StringBuffer columnNames = new StringBuffer(200);
            StringBuffer columnsValues = new StringBuffer(200);
            sqlInsert.append("INSERT INTO ").append(itemName.toString()).append(" (");
            for (int i = 0; i < columnsCount; i++) {
                columnNames.append( meta.getColumnLabel(i + 1));
                columnsValues.append("?");
                if (i != columnsCount - 1) {
                    columnNames.append(",");
                    columnsValues.append(",");
                }
            }
            sqlInsert.append(columnNames).append(") VALUES").append("(").append(columnsValues).append(")");
            log.debug("SQL = {}", sqlInsert.toString()); // composed insert
            // precompile insert SQL
            preparedInsertStatement = con.prepareStatement(sqlInsert.toString());

            // loop over CSV data reading line by line, column by column
            while (rs.next()) {
                for (int i = 0; i < columnsCount; i++) {
                    Object object = rs.getObject(i + 1);
                    log.trace("{}[{} : {}] = {}", meta.getColumnName(i + 1), i + 1, meta.getColumnTypeName(i + 1), object);

                    if (object != null && (meta.getColumnType(i + 1) == Types.BINARY || meta.getColumnType(i + 1) == Types.VARBINARY)) {
                        InputStream is = null;
                        try {
                            is = new ByteArrayInputStream( parser.parseBinaryObject(object) );
                            preparedInsertStatement.setBinaryStream(i + 1, is, meta.getPrecision(i + 1));
                        } catch (SQLException e) {
                            log.error("Binary/Varbinary reading error = " + object, e);
                            throw e;
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e) {} // ignore error here
                            }
                        }
                    } else if (object != null && (meta.getColumnType(i + 1) == Types.ARRAY)) {
                        preparedInsertStatement.setObject(i + 1, object);
                    } else {
                        preparedInsertStatement.setObject(i + 1, object);
                    }
                }
                log.trace("sql = {}", sqlInsert);
                importedCount += preparedInsertStatement.executeUpdate();
                if (batchLimit % importedCount == 0) {
                    con.commit();
                }
            }
            con.commit(); // final commit
        } catch (Exception e) {
            log.error("Error on importing data on table = '{}'", itemName, e);
            throw e;
        } finally {
            if (preparedInsertStatement != null) {
                DbUtils.close(preparedInsertStatement);
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
        try (Connection con = extension.getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("delete from " + itemName + " where db_id  BETWEEN ? AND ?")) {
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
            CsvReader csvReader = new CsvReaderImpl(null, translator);
        });

        CsvReader csvReader = new CsvReaderImpl(dirProvider.getDataExportDir(), translator);
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
            CsvWriter csvWriter = new CsvWriterImpl(null, Collections.emptySet(), translator);
        });

        CsvWriter csvWriter = new CsvWriterImpl(dirProvider.getDataExportDir(), Collections.emptySet(), translator);
        csvWriter.setOptions("fieldDelimiter="); // do not put ""

        String tableName = "unknown_table_name";
        assertThrows(NullPointerException.class, () -> csvWriter.write(tableName + CSV_FILE_EXTENSION, null));
    }

    @Test
    void testAppendWithDefaultParameters() throws SQLException {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("csvExport").toPath()).when(dirProvider).getDataExportDir();
        CsvWriter csvWriter = new CsvWriterImpl(dirProvider.getDataExportDir(), Set.of("DB_ID"), translator);
        csvWriter.setOptions("fieldDelimiter=");

        CsvExportData csvExportData = csvWriter.append("public_key", extension.getDatabaseManager().getDataSource().getConnection().createStatement().executeQuery("select * from public_key where height<= 8000"), Map.of("account_id", "batman", "public_key", "null"));

        int processCount = csvExportData.getProcessCount();
        assertEquals(8, processCount);
        assertEquals(Map.of("PUBLIC_KEY", "null", "ACCOUNT_ID", "batman", "HEIGHT", 8000, "LATEST", Boolean.TRUE, "DB_ID", 8L), csvExportData.getLastRow());

        CsvReader csvReader = new CsvReaderImpl(dirProvider.getDataExportDir(), translator);
        ResultSet rs = csvReader.read("public_key", null, null);
        while (rs.next()) {
            Object publicKey = rs.getObject("public_key");
            assertNull(publicKey);
            Object accountId= rs.getObject("account_id");
            assertEquals("batman", accountId);
        }
    }
}