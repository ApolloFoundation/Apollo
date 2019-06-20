/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.ShardDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriterImpl;
import org.slf4j.Logger;

/**
 * {@inheritDoc}
 */
@Singleton
public class CsvExporterImpl implements CsvExporter {
    private static final Logger log = getLogger(CsvExporterImpl.class);
    private Path dataExportPath; // path to folder with CSV files
    private DatabaseManager databaseManager;
    private ShardDaoJdbc shardDaoJdbc;

    private Set<String> excludeTables; // skipped tables

    @Inject
    public CsvExporterImpl(DatabaseManager databaseManager, @Named("dataExportDir") Path dataExportPath, ShardDaoJdbc shardDaoJdbc) {
        Objects.requireNonNull(dataExportPath, "exportDirProducer 'data Path' is NULL");
        this.dataExportPath = dataExportPath;
        try {
            boolean folderExist = Files.exists(this.dataExportPath);
            if (!folderExist) { // check and create dataExport folder
                Files.createDirectory(this.dataExportPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create data export directory", e);
        }
        //        this.dataExportPath = Objects.requireNonNull(dataExportPath, "data export Path is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.shardDaoJdbc = Objects.requireNonNull(shardDaoJdbc, "shardDaoJdbc is NULL");
        this.excludeTables = Set.of("genesis_public_key");
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
    public long exportDerivedTable(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit) {
        Objects.requireNonNull(derivedTableInterface, "derivedTableInterface is NULL");
        // skip hard coded table
        if (excludeTables.contains(derivedTableInterface.toString().toLowerCase())) {
            // skip not needed table
            log.debug("Skipped excluded Table = {}", derivedTableInterface.toString());
            return -1;
        }

        long start = System.currentTimeMillis();
        int processedCount;
        int totalCount = 0;
        // prepare connection + statement + writer
        try (Connection con = this.databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "select * from " + derivedTableInterface.toString() + " where db_id > ? and db_id < ? order by db_id limit ?");
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, Set.of("DB_ID", "LATEST"))
        ) {

            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv            // select Min, Max DbId + rows count
            MinMaxDbId minMaxDbId = derivedTableInterface.getMinMaxDbId(targetHeight);
            log.debug("Table = {}, Min/Max = {} at height = {}", derivedTableInterface.toString(), minMaxDbId, targetHeight);

            // process non empty tables only
            if (minMaxDbId.getCount() > 0) {
                do { // do exporting into csv with pagination
                    CsvExportData csvExportData = csvWriter.append(derivedTableInterface.toString(),
                            derivedTableInterface.getRangeByDbId(con, pstmt, minMaxDbId, batchLimit));
                    processedCount = csvExportData.getProcessCount();
                    if (processedCount > 0) {
                        minMaxDbId.setMinDbId((Long) csvExportData.getLastRow().get("DB_ID"));
                    }
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {} in {} sec", derivedTableInterface.toString(), totalCount,
                        (System.currentTimeMillis() - start) / 1000);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", derivedTableInterface.toString());
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Exporting derived table exception " + derivedTableInterface.toString(), e);
        }

        return totalCount;
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
                     "SELECT shard_id, shard_hash, shard_height, zip_hash_crc, generator_ids FROM shard WHERE shard_id > ? AND shard_id < ? ORDER BY shard_id LIMIT ?");
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, null)
        ) {
            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv            // select Min, Max DbId + rows count            // select Min, Max DbId + rows count
            MinMaxDbId minMaxDbId = shardDaoJdbc.getMinMaxId(dataSource, targetHeight);
            log.debug("Table = {}, Min/Max = {} at height = {}", ShardConstants.SHARD_TABLE_NAME, minMaxDbId, targetHeight);

            // process non empty tables only
            if (minMaxDbId.getCount() > 0) {
                do { // do exporting into csv with pagination
                    CsvExportData csvExportData = csvWriter.append(ShardConstants.SHARD_TABLE_NAME,
                            shardDaoJdbc.getRangeByDbId(con, pstmt, minMaxDbId, batchLimit));
                    processedCount = csvExportData.getProcessCount();
                    if (processedCount > 0) {
                        minMaxDbId.setMinDbId((Long) csvExportData.getLastRow().get("SHARD_ID"));
                    }
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {}", ShardConstants.SHARD_TABLE_NAME, totalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", ShardConstants.SHARD_TABLE_NAME);
            }
        } catch (Exception e) {
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
}
