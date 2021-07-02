/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyBasedFileConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountLedgerTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
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
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionCreator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.TableRegistryInitializer;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaperImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriterImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionVersionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ServiceModeDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase.CSV_FILE_EXTENSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class CsvWriterReaderDerivedTablesTest extends DbContainerBaseTest {

    @RegisterExtension
    TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, Map.of("currency", List.of("code", "name", "description"), "tagged_data", List.of("name", "description", "tags")));
    @Inject
    Event<DeleteOnTrimData> deleteOnTrimDataEvent;

    BlockchainConfig blockchainConfig = mockBlockchainConfig();
    PropertiesHolder propertiesHolder = mockPropertiesHolder();
    NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    PeersService peersService = mock(PeersService.class);
    GeneratorService generatorService = mock(GeneratorService.class);
    TransactionTestData td = new TransactionTestData();
    BlockSerializer blockSerializer = mock(BlockSerializer.class);
    MemPool memPool = mock(MemPool.class);
    UnconfirmedTransactionProcessingService unconfirmedTransactionProcessingService = mock(UnconfirmedTransactionProcessingService.class);
    PublicKeyDao publicKeyDao = mock(PublicKeyDao.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        BlockchainImpl.class, DaoConfig.class,
        PropertyProducer.class, TransactionApplier.class, ServiceModeDirProvider.class, PublicKeyTableProducer.class,
        TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class, TransactionEntityRowMapper.class,
        GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class, TransactionModelToEntityConverter.class, ShardDbExplorerImpl.class, PrunableTxRowMapper.class, TransactionEntityToModelConverter.class, UnconfirmedTransactionEntityRowMapper.class, TxReceiptRowMapper.class, TransactionServiceImpl.class, TransactionEntityRowMapper.class,
        ReferencedTransactionDaoImpl.class,
        TaggedDataTable.class, PropertyBasedFileConfig.class,
        DGSGoodsTable.class,
        AppendixApplierRegistry.class,
        AppendixValidatorRegistry.class,
        TransactionBuilderFactory.class, DexOrderTable.class,TableRegistryInitializer.class,
        UnconfirmedTransactionCreator.class, AccountGuaranteedBalanceTable.class, DGSPurchaseTable.class, DexContractTable.class,
        TaggedDataTable.class, AccountCurrencyTable.class, AccountAssetTable.class,  PublicKeyTable.class, AccountLedgerTable.class,
        PropertyBasedFileConfig.class,
        DataTagDao.class, PhasingPollResultTable.class, GenesisPublicKeyTable.class,
        UnconfirmedTransactionCreator.class,
        FeeCalculator.class,
        TaggedDataTimestampDao.class,
        TaggedDataExtendDao.class,
        FullTextConfigImpl.class,
        DerivedDbTablesRegistryImpl.class,
        DirProvider.class,
        AplAppStatus.class,
        PhasingPollResultTable.class,
        PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class,
        PhasingVoteTable.class, PhasingPollTable.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class,
        CsvEscaperImpl.class, UnconfirmedTransactionTable.class, AccountService.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getFullTextSearchService(), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(TrimService.class), TrimService.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
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
        .addBeans(MockBean.of(mock(TransactionVersionValidator.class), TransactionVersionValidator.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(blockSerializer, BlockSerializer.class))
        .addBeans(MockBean.of(publicKeyDao, PublicKeyDao.class))
        .addBeans(MockBean.of(unconfirmedTransactionProcessingService, UnconfirmedTransactionProcessingService.class))
        .addBeans(MockBean.of(memPool, MemPool.class))
        .addBeans(MockBean.of(mock(InMemoryCacheManager.class), InMemoryCacheManager.class))
        .addBeans(MockBean.of(mock(FullTextSearchUpdater.class), FullTextSearchUpdater.class))
        .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
        .build();

    @Inject
    DerivedTablesRegistry derivedTablesRegistry;
    @Inject
    CsvEscaper translator;
    @Inject
    TableRegistryInitializer tableRegistryInitializer;
    @Inject
    @Named("genesisPublicKeyTable")
    EntityDbTableInterface<PublicKey> genesisPublicKeyTable;
    @Inject
    AccountAssetTable accountAssetTable;
    @Inject
    AccountCurrencyTable accountCurrencyTable;
    @Inject
    @Named("publicKeyTable")
    EntityDbTableInterface<PublicKey> publicKeyTable;
    @Inject
    AccountLedgerTable accountLedgerTable;

    @Inject
    AccountGuaranteedBalanceTable accountGuaranteedBalanceTable;
    @Inject
    DGSPurchaseTable purchaseTable;
    @Inject
    DexContractTable dexContractTable;
    @Inject
    DexOrderTable dexOrderTable;

    public CsvWriterReaderDerivedTablesTest() throws Exception {
    }


    @Tag("skip-fts-init")
    @DisplayName("Gather all derived tables, export data up to height = 8000," +
        " delete rows up to height = 8000, import data back into db table")
    void testExportAndImportData() {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("csvExport").toPath()).when(dirProvider).getDataExportDir();
        // init columns excludes from export
        Set<String> excludeColumnNames = Set.of("DB_ID", "LATEST");
        // init Cvs reader, writer components

        Collection<DerivedTableInterface<? extends DerivedEntity>> result = derivedTablesRegistry.getDerivedTables(); // extract all derived tables

        assertNotNull(result);
        log.debug("Processing [{}] tables", result.size());
//        assertEquals(12, result.size()); // the real number is higher then initial, it's OK !
        int targetHeight = Integer.MAX_VALUE;
        result.forEach(item -> {
            assertNotNull(item);
            log.debug("Table = '{}'", item.toString());
            BigDecimal minDbValue = BigDecimal.ZERO;
            BigDecimal maxDbValue = BigDecimal.ZERO;
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
                assertTrue(minMaxValue.getMax().longValue() >= 0);
                log.debug("Table = {}, Min/Max = {} at height = {}", item.toString(), minMaxValue, targetHeight);

                // process non empty tables
                if (minMaxValue.getCount() > 0) {
                    do { // do exporting into csv with pagination
                        CsvExportData csvExportData = csvWriter.append(item.toString(),
                            item.getRangeByDbId(con, pstmt, minMaxValue, batchLimit));

                        processedCount = csvExportData.getProcessCount();
                        if (processedCount > 0) {
                            Object rawObjectIdValue = csvExportData.getLastRow().get("db_id");
                            if (rawObjectIdValue instanceof BigInteger) {
                                BigDecimal bidDecimalId = new BigDecimal((BigInteger) rawObjectIdValue);
                                bidDecimalId = bidDecimalId.add(BigDecimal.ONE);
                                minMaxValue.setMin(bidDecimalId);
                            } else {
                                String error = "Something different then BigDecimal in type = " + rawObjectIdValue.getClass();
                                log.error(error);
                                throw new InvalidClassException(error);
                            }
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
     * @param itemName   table name
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
                columnNames.append(meta.getColumnLabel(i + 1));
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
                    log.info(",{}", object);
                    log.trace("{}[{} : {}] = {}", meta.getColumnName(i + 1), i + 1, meta.getColumnTypeName(i + 1), object);

                    if (object != null && (meta.getColumnType(i + 1) == Types.BINARY || meta.getColumnType(i + 1) == Types.VARBINARY)) {
                        InputStream is = null;
                        try {
                            is = new ByteArrayInputStream(parser.parseBinaryObject(object));
                            preparedInsertStatement.setBinaryStream(i + 1, is, meta.getPrecision(i + 1));
                        } catch (SQLException e) {
                            log.error("Binary/Varbinary reading error = " + object, e);
                            throw e;
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                } // ignore error here
                            }
                        }
                    } else if (object != null && (meta.getColumnType(i + 1) == Types.ARRAY)) {
                        preparedInsertStatement.setObject(i + 1, object);
                    } else if (object != null && (meta.getColumnType(i + 1) == Types.LONGVARCHAR)) {

                        String unescaped = translator.unEscape(object.toString()); // unescape
                        unescaped = unescaped.substring(1, unescaped.length() - 1); // remove quotes
                        preparedInsertStatement.setString(i + 1, unescaped);
                    } else {
                        preparedInsertStatement.setObject(i + 1, object);
                    }
                }
                log.info("sql = {}", sqlInsert);
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
     *
     * @param minDbValue min db id
     * @param maxDbValue max db id
     * @param itemName   derived table name
     * @return deleted rows quantity
     */
    private int dropDataByName(BigDecimal minDbValue, BigDecimal maxDbValue, String itemName) {
        // drop data
        try (Connection con = extension.getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("delete from " + itemName + " where db_id  BETWEEN ? AND ?")) {
            pstmt.setBigDecimal(1, minDbValue);
            pstmt.setBigDecimal(2, maxDbValue);
            int deleted = pstmt.executeUpdate();
            con.commit();
            log.debug("Table = {}, deleted = {} by MIN = {} / MAX = {}", itemName, deleted, minDbValue, maxDbValue);
            return deleted;
        } catch (SQLException e) {
            log.error("Exception", e);
        }
        return -1;
    }
//    con.createStatement().executeUpdate("INSERT INTO tagged_data (id,account_id,name,description,tags,parsed_tags,type,data,is_text,channel,filename,block_timestamp,transaction_timestamp,height,latest) VALUES(-780794814210884355,9211698109297098287,'tag1','tag1' 'descr','tag1 tag2 tag3 tag2 sl','[\"tag1\",\"tag2\",\"tag3\"]',null,null,1,null,null,18400,35078473,2000)");

    @Tag("skip-fts-init")
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

    @Tag("skip-fts-init")
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

    @Tag("skip-fts-init")
    @Test
    void testAppendWithDefaultParameters() throws SQLException {
        DirProvider dirProvider = mock(DirProvider.class);
        doReturn(temporaryFolderExtension.newFolder("csvExport").toPath()).when(dirProvider).getDataExportDir();
        CsvWriter csvWriter = new CsvWriterImpl(dirProvider.getDataExportDir(), Set.of("DB_ID"), translator);
        csvWriter.setOptions("fieldDelimiter=");

        CsvExportData csvExportData = csvWriter.append("public_key", extension.getDatabaseManager().getDataSource().getConnection().createStatement().executeQuery("select * from public_key where height<= 8000"), Map.of("account_id", "batman", "public_key", "null"));

        int processCount = csvExportData.getProcessCount();
        assertEquals(8, processCount);
        HashMap<String, Object> expectedHashMap = new LinkedHashMap<>(5);
        expectedHashMap.put("public_key", "null");
        expectedHashMap.put("db_id", BigInteger.valueOf(8L));
        expectedHashMap.put("account_id", "batman");
        expectedHashMap.put("height", 8000);
        expectedHashMap.put("latest", Boolean.TRUE);
        assertEquals(expectedHashMap.keySet(), csvExportData.getLastRow().keySet());
        assertIterableEquals(expectedHashMap.values(), csvExportData.getLastRow().values());

        CsvReader csvReader = new CsvReaderImpl(dirProvider.getDataExportDir(), translator);
        ResultSet rs = csvReader.read("public_key", null, null);
        while (rs.next()) {
            Object publicKey = rs.getObject("public_key");
            assertNull(publicKey);
            Object accountId = rs.getObject("account_id");
            assertEquals("batman", accountId);
        }
    }
    BlockchainConfig mockBlockchainConfig() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        HeightConfig config = mock(HeightConfig.class);
        Chain chain = mock(Chain.class);
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(chain).getChainId();
        return blockchainConfig;
    }

    private PropertiesHolder mockPropertiesHolder() {
        PropertiesHolder holder = mock(PropertiesHolder.class);
        doReturn(21).when(holder).getIntProperty("apl.derivedTablesCount", 55);
        return holder;
    }
}