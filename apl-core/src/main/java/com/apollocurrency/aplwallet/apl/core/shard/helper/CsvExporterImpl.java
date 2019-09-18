/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriterImpl;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@inheritDoc}
 */
@Singleton
public class CsvExporterImpl implements CsvExporter {
    private static final Logger log = getLogger(CsvExporterImpl.class);
    private static final Set<String> DEFAULT_EXCLUDED_COLUMNS = Set.of("DB_ID", "LATEST");
    private Path dataExportPath; // path to folder with CSV files
    private DatabaseManager databaseManager;
    private Set<String> excludeTables; // skipped tables

    @Inject
    public CsvExporterImpl(DatabaseManager databaseManager, @Named("dataExportDir") Path dataExportPath) {
        Objects.requireNonNull(dataExportPath, "exportDirProducer 'data Path' is NULL");
        this.dataExportPath = dataExportPath;
        try {
             Files.createDirectories(this.dataExportPath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create data export directory", e);
        }
        //        this.dataExportPath = Objects.requireNonNull(dataExportPath, "data export Path is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.excludeTables = Set.of(ShardConstants.GENESIS_PK_TABLE_NAME, ShardConstants.DATA_TAG_TABLE_NAME, ShardConstants.UNCONFIRMED_TX_TABLE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getDataExportPath() {
        return this.dataExportPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportDerivedTable(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit, Set<String> excludedColumns) {
        return exportDerivedTableByUniqueLongColumnPagination(derivedTableInterface.getName(), derivedTableInterface.getMinMaxValue(targetHeight), batchLimit, excludedColumns);
    }

    private long exportDerivedTableByUniqueLongColumnPagination(String table, MinMaxValue minMaxValue, int batchLimit, Set<String> excludedColumns) {
        return exportTable(table,"where  " + minMaxValue.getColumn() + "> ? and " + minMaxValue.getColumn() +"< ? and height <= ? order by " + minMaxValue.getColumn() + " limit ?", minMaxValue, excludedColumns, (pstmt, minMaxColumnValue, totalProcessed) -> {
            pstmt.setLong(1,  minMaxColumnValue.getMin());
            pstmt.setLong(2,  minMaxColumnValue.getMax());
            pstmt.setInt(3,  minMaxColumnValue.getHeight());
            pstmt.setInt(4, batchLimit);
        });
    }

    @Override
    public long exportDerivedTableCustomSort(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit, Set<String> excludedColumns, String sortColumn) {
        return exportTable(derivedTableInterface.getName(),"where height <= ? order by " + sortColumn + " LIMIT ? OFFSET ?", derivedTableInterface.getMinMaxValue(targetHeight), excludedColumns, (pstmt, minMaxId, totalProcessed) -> {
            pstmt.setInt(1,  targetHeight);
            pstmt.setInt(2,  batchLimit);
            pstmt.setInt(3, totalProcessed);
        });
    }

    @Override
    public long exportDerivedTable(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit) {
        return exportDerivedTable(derivedTableInterface, targetHeight, batchLimit, DEFAULT_EXCLUDED_COLUMNS);
    }

    @Override
    public long exportPrunableDerivedTable(PrunableDbTable table, int targetHeight, int currentTime, int batchLimit) {
        return exportDerivedTableByUniqueLongColumnPagination(table.getName(), table.getMinMaxValue(targetHeight, currentTime), batchLimit, DEFAULT_EXCLUDED_COLUMNS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportShardTable(int targetHeight, int batchLimit) {
        int processedCount;
        int totalCount = 0;
        // prepare connection + statement + writer
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "SELECT * FROM shard WHERE shard_id > ? AND shard_height <= ? ORDER BY shard_id LIMIT ?");
             PreparedStatement countPstmt = con.prepareStatement("SELECT count(*) FROM shard WHERE shard_height <= ?");
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, Set.of("SHARD_STATE"))
        ) {
            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
            // select Min, Max DbId + rows count
            countPstmt.setInt(1, targetHeight);
            ResultSet countRs = countPstmt.executeQuery();
            countRs.next();
            int count = countRs.getInt(1);
            log.debug("Table = {},count - {} at height = {}", ShardConstants.SHARD_TABLE_NAME, count, targetHeight);

            // process non empty tables only
            if (count > 0) {
                long from = 0;
                do { // do exporting into csv with pagination
                    pstmt.setLong(1, from);
                    pstmt.setInt(2, targetHeight);
                    pstmt.setInt(3, batchLimit);
                    CsvExportData csvExportData = csvWriter.append(ShardConstants.SHARD_TABLE_NAME, pstmt.executeQuery());
                    processedCount = csvExportData.getProcessCount();
                    if (processedCount > 0) {
                        from = (Long) csvExportData.getLastRow().get("SHARD_ID");
                    }
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {}", ShardConstants.SHARD_TABLE_NAME, totalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", ShardConstants.SHARD_TABLE_NAME);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Exporting table exception " + ShardConstants.SHARD_TABLE_NAME, e);
        }
        return totalCount;
    }

    @Override
    public long exportShardTableIgnoringLastZipHashes(int targetHeight, int batchLimit) {
        int totalCount = 0;
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM shard WHERE shard_height <= ? ORDER BY shard_id DESC LIMIT 1")) {
            pstmt.setInt(1, targetHeight);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int height = rs.getInt("shard_height");
                    totalCount += exportShardTable(height - 1, batchLimit);
                    try (CsvWriter csvWriter = new CsvWriterImpl(dataExportPath, Set.of("SHARD_STATE"))) {
                        csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
                        csvWriter.append(ShardConstants.SHARD_TABLE_NAME, pstmt.executeQuery(), Map.of("zip_hash_crc", "null", "prunable_zip_hash", "null"));
                        totalCount += 1;
                    }
                } else {
                    log.debug("Skipped exporting Table = {}", ShardConstants.SHARD_TABLE_NAME);
                }
                log.trace("Table = {}, exported rows = {}", ShardConstants.SHARD_TABLE_NAME, totalCount);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Exporting table exception " + ShardConstants.SHARD_TABLE_NAME, e);
        }
        return totalCount;
    }

    @Override
    public long exportBlockIndex(int targetHeight, int batchLimit) {
        int blockProcessedCount;
        int blockTotalCount = 0;
        // prepare connection + statement + writer
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement blockPstm = con.prepareStatement(
                     "select * from block_index where block_height >= ? and block_height < ? order by block_height limit ?");
             PreparedStatement blockCountPstm = con.prepareStatement("select count(*) from block_index");
             CsvWriter blockCsvWriter = new CsvWriterImpl(this.dataExportPath, null)
        ) {
            blockCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv

            // process non empty tables only
            int blockCount;
            try (ResultSet rs = blockCountPstm.executeQuery()) {
                rs.next();
                blockCount = rs.getInt(1);
            }
            if (blockCount > 0) {
                int fromHeight = 0;
                do { // do exporting into csv with pagination
                    blockPstm.setInt(1, fromHeight);
                    blockPstm.setInt(2, targetHeight);
                    blockPstm.setInt(3, batchLimit);
                    CsvExportData csvExportData = blockCsvWriter.append(ShardConstants.BLOCK_INDEX_TABLE_NAME,
                            blockPstm.executeQuery());
                    blockProcessedCount = csvExportData.getProcessCount();
                    if (blockProcessedCount > 0) {
                        int lastHeight = (int) csvExportData.getLastRow().get("BLOCK_HEIGHT");
                        fromHeight = lastHeight + 1;
                    }
                    blockTotalCount += blockProcessedCount;
                } while (blockProcessedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {}", ShardConstants.BLOCK_INDEX_TABLE_NAME, blockTotalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", ShardConstants.BLOCK_INDEX_TABLE_NAME);
            }
        } catch (Exception e) {
            log.error("Error", e);
            throw new RuntimeException("Exporting exception " + ShardConstants.BLOCK_INDEX_TABLE_NAME, e);
        }
        return blockTotalCount;
    }

    @Override
    public long exportTransactionIndex(int targetHeight, int batchLimit) {
        int txTotalCount = 0;
        // prepare connection + statement + writer
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement txPstm = con.prepareStatement(
                     "select * from transaction_shard_index where (height = ? and transaction_index > ? or height > ?) and height < ? order by height, transaction_index limit ?");
             PreparedStatement txCountPstm = con.prepareStatement("select count(*) from transaction_shard_index");
             CsvWriter txCsvWriter = new CsvWriterImpl(this.dataExportPath, null)
        ) {
            txCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
            int txCount;
            try (ResultSet rs = txCountPstm.executeQuery()) {
                rs.next();
                txCount = rs.getInt(1);
            }
            if (txCount > 0) {
                int fromHeight = 0;
                int txIndex = 0;
                int txProcessCount;
                do { // do exporting into csv with pagination
                    txPstm.setInt(1, fromHeight);
                    txPstm.setInt(2, txIndex);
                    txPstm.setInt(3, fromHeight);
                    txPstm.setInt(4, targetHeight);
                    txPstm.setInt(5, batchLimit);
                    CsvExportData transactionExportData = txCsvWriter.append(ShardConstants.TRANSACTION_INDEX_TABLE_NAME, txPstm.executeQuery());
                    txProcessCount = transactionExportData.getProcessCount();

                    txTotalCount += txProcessCount;
                    if (txProcessCount > 0) {
                        fromHeight = (int) transactionExportData.getLastRow().get("HEIGHT");
                        txIndex = (short) transactionExportData.getLastRow().get("TRANSACTION_INDEX");
                    }

                } while (txProcessCount > 0);
                log.trace("Exported rows = {} from {}", txTotalCount, ShardConstants.TRANSACTION_INDEX_TABLE_NAME);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", ShardConstants.TRANSACTION_INDEX_TABLE_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException("Exporting table exception " + ShardConstants.TRANSACTION_INDEX_TABLE_NAME, e);
        }
        return txTotalCount;
    }

    @Override
    public long exportTransactions(Collection<Long> dbIds) {
        int processCount;
        int totalCount = 0;
        // prepare connection + statement + writer
        List<Long> sortedDbIds = dbIds.stream().distinct().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement txPstm = con.prepareStatement(
                     "select * from transaction where db_id = ?");
             CsvWriter txWriter = new CsvWriterImpl(this.dataExportPath, Set.of("DB_ID"))
        ) {
            txWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv

            // process non empty tables only
            if (sortedDbIds.size() > 0) {
                for (Long dbId : sortedDbIds) {
                    txPstm.setLong(1, dbId);
                    CsvExportData csvExportData = txWriter.append(ShardConstants.TRANSACTION_TABLE_NAME,
                            txPstm.executeQuery());
                    processCount = csvExportData.getProcessCount();
                    totalCount += processCount;
                }
                log.trace("Table = {}, exported rows = {}", ShardConstants.TRANSACTION_TABLE_NAME, totalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", ShardConstants.TRANSACTION_TABLE_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException("Exporting table exception " + ShardConstants.TRANSACTION_TABLE_NAME, e);
        }
        return totalCount;
    }

    @Override
    public long exportBlock(int height) throws IllegalStateException {
        int processCount;
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement txPstm = con.prepareStatement("select * from transaction where height = ? order by transaction_index");
             PreparedStatement blockPstm = con.prepareStatement(
                     "select * from block where height = ?");
             CsvWriter blockCsvWriter = new CsvWriterImpl(this.dataExportPath, Set.of("DB_ID"));
             CsvWriter txCsvWriter = new CsvWriterImpl(this.dataExportPath, Set.of("DB_ID"))
        ) {
            txPstm.setInt(1, height);
            blockPstm.setInt(1, height);
            txCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
            blockCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
            CsvExportData blockExportData = blockCsvWriter.append(ShardConstants.BLOCK_TABLE_NAME, blockPstm.executeQuery());
            if (blockExportData.getProcessCount() != 1) {
                Files.deleteIfExists(dataExportPath.resolve(ShardConstants.BLOCK_TABLE_NAME + ".csv"));
                throw new IllegalStateException("Expected one exported block, got " + blockExportData.getProcessCount());
            }
            CsvExportData txExportData = txCsvWriter.append(ShardConstants.TRANSACTION_TABLE_NAME, txPstm.executeQuery());
            processCount = txExportData.getProcessCount() + blockExportData.getProcessCount(); // tx + block
        }
        catch (SQLException | IOException e) {
            throw new RuntimeException("Exporting table exception " + ShardConstants.BLOCK_TABLE_NAME, e);
        }
        return processCount;
    }

    private long exportTable(String table, String condition, MinMaxValue minMaxValue, Set<String> excludedColumns, StatementConfigurator statementConfigurator) {
        Objects.requireNonNull(condition, "Condition sql should not be null");
        Objects.requireNonNull(table, "Table should not be null");
        Objects.requireNonNull(minMaxValue, "MinMaxValue should not be null");
        // skip hard coded table
        if (excludeTables.contains(table.toLowerCase())) {
            // skip not needed table
            log.debug("Skipped excluded Table = {}", table);
            return -1;
        }

        long start = System.currentTimeMillis();
        int processedCount;
        int totalCount = 0;
        // prepare connection + statement + writer
        try (Connection con = this.databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "select * from " + table + " " + condition);
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, excludedColumns)
        ) {

            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv            // select Min, Max DbId + rows count
            log.debug("Table = {}, Min/Max = {}", table, minMaxValue);

            // process non empty tables only
            if (minMaxValue.getCount() > 0) {
                do { // do exporting into csv with pagination
                    statementConfigurator.configure(pstmt, minMaxValue, totalCount);
                    CsvExportData csvExportData = csvWriter.append(table, pstmt.executeQuery());
                    processedCount = csvExportData.getProcessCount();
                    if (processedCount > 0) {
                        minMaxValue.setMin((Long) csvExportData.getLastRow().get(minMaxValue.getColumn().toUpperCase()));
                    }
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {} in {} sec", table, totalCount,
                        (System.currentTimeMillis() - start) / 1000);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", table);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Exporting derived table exception " + table, e);
        }

        return totalCount;
    }
    private interface StatementConfigurator {
        void configure(PreparedStatement pstmt, MinMaxValue minMaxValue, int totalProcessed) throws SQLException;
    }
}
