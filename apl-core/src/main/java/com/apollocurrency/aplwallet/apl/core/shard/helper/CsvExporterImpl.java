/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.ShardDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriterImpl;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@inheritDoc}
 */
@Singleton
public class CsvExporterImpl implements CsvExporter {
    private static final Logger log = getLogger(CsvExporterImpl.class);

    private Path dataExportPath; // path to folder with CSV files
    private DatabaseManager databaseManager;
    private ShardDaoJdbc shardDaoJdbc;

    private Set<String> excludeColumnNamesInDerivedTables; // excluded column
    private Set<String> excludeTables; // skipped tables

    @Inject
    public CsvExporterImpl(DatabaseManager databaseManager, @Named("dataExportDir") Path dataExportPath, ShardDaoJdbc shardDaoJdbc) {
        Objects.requireNonNull(dataExportPath, "exportDirProducer 'data Path' is NULL");
        this.dataExportPath = dataExportPath;
//        this.dataExportPath = Objects.requireNonNull(dataExportPath, "data export Path is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.shardDaoJdbc = Objects.requireNonNull(shardDaoJdbc, "shardDaoJdbc is NULL");
        excludeColumnNamesInDerivedTables = Set.of("DB_ID");
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
                     "select * from " + derivedTableInterface.toString() + " where db_id > ? and db_id < ? limit ?");
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, excludeColumnNamesInDerivedTables, "DB_ID");
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
                        minMaxDbId.setMinDbId((Long) csvExportData.getLastKey());
                    }
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {} in {} sec", derivedTableInterface.toString(), totalCount,
                        (System.currentTimeMillis() - start) / 1000);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", derivedTableInterface.toString());
            }
        } catch (Exception e) {
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
        String tableName = "shard";
        // prepare connection + statement + writer
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "select * from SHARD where shard_id > ? and shard_id < ? limit ?");
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, excludeColumnNamesInDerivedTables, "SHARD_ID");
             ) {
            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv            // select Min, Max DbId + rows count            // select Min, Max DbId + rows count
            MinMaxDbId minMaxDbId = shardDaoJdbc.getMinMaxId(dataSource, targetHeight);
            log.debug("Table = {}, Min/Max = {} at height = {}", tableName, minMaxDbId, targetHeight);

            // process non empty tables only
            if (minMaxDbId.getCount() > 0) {
                do { // do exporting into csv with pagination
                    CsvExportData csvExportData = csvWriter.append(tableName,
                            shardDaoJdbc.getRangeByDbId(con, pstmt, minMaxDbId, batchLimit));
                    processedCount = csvExportData.getProcessCount();
                    if (processedCount > 0) {
                        minMaxDbId.setMinDbId((Long) csvExportData.getLastKey());
                    }
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {}", tableName, totalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", tableName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Exporting derived table exception " + tableName, e);
        }
        return totalCount;
    }

    @Override
    public IndexExportData exportIndexes(int targetHeight, int batchLimit) {
        int blockProcessedCount;
        int blockTotalCount = 0;
        int txTotalCount = 0;
        String tableName = "block_index";
        // prepare connection + statement + writer
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement blockPstm = con.prepareStatement(
                     "select * from block_index where block_height >= ? and block_height < ? limit ?");
             PreparedStatement txPstm = con.prepareStatement(
                     "select * from transaction_shard_index where height = ? order by transaction_index");
             PreparedStatement blockCountPstm = con.prepareStatement("select count(*) from block_index");
             CsvWriter blockCsvWriter = new CsvWriterImpl(this.dataExportPath, null, "BLOCK_HEIGHT");
             CsvWriter txCsvWriter = new CsvWriterImpl(this.dataExportPath, null, "HEIGHT")
        ) {
            blockCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
            txCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv

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
                    CsvExportData csvExportData = blockCsvWriter.append(tableName,
                            blockPstm.executeQuery());
                    blockProcessedCount = csvExportData.getProcessCount();
                    if (blockProcessedCount > 0) {
                        int lastHeight = (int) csvExportData.getLastKey();
                        for (int i = fromHeight; i <= lastHeight; i++) {
                            txPstm.setInt(1, i);
                            CsvExportData transactionExportData = txCsvWriter.append("transaction_shard_index", txPstm.executeQuery());
                            txTotalCount += transactionExportData.getProcessCount();
                        }
                        fromHeight = lastHeight + 1;
                    }
                    blockTotalCount += blockProcessedCount;
                } while (blockProcessedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {}", tableName, blockTotalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", tableName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Exporting derived table exception " + tableName, e);
        }
        return new IndexExportData(txTotalCount, blockTotalCount);
    }
}
