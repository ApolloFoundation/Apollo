/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.ShardDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
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
    private Set<String> excludeColumnNamesInDerivedTables; // excluded column
    private Set<String> excludeTables; // skipped tables

    @Inject
    public CsvExporterImpl(@Named("dataExportDir") Path dataExportPath, DatabaseManager databaseManager,
                           ShardDaoJdbc shardDaoJdbc) {
        this.dataExportPath = Objects.requireNonNull(dataExportPath, "data export Path is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.shardDaoJdbc = Objects.requireNonNull(shardDaoJdbc, "shardDaoJdbc is NULL");
        excludeColumnNamesInDerivedTables = Set.of("DB_ID");
        this.excludeTables = Set.of("genesis_public_key");
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
        int processedCount = 0;
        int totalCount = 0;
        // prepare connection + statement + writer
        try (Connection con = this.databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "select * from " + derivedTableInterface.toString() + " where db_id > ? and db_id < ? limit ?");
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, excludeColumnNamesInDerivedTables, "DB_ID");
        ) {
            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes 'comma' around values in csv            // select Min, Max DbId + rows count
            MinMaxDbId minMaxDbId = derivedTableInterface.getMinMaxDbId(targetHeight);
            log.debug("Table = {}, Min/Max = {} at height = {}", derivedTableInterface.toString(), minMaxDbId, targetHeight);

            // process non empty tables only
            if (minMaxDbId.getCount() > 0) {
                do { // do exporting into csv with pagination
                    processedCount = csvWriter.append(derivedTableInterface.toString(),
                            derivedTableInterface.getRangeByDbId(con, pstmt, minMaxDbId, batchLimit), minMaxDbId );
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {} in {} sec", derivedTableInterface.toString(), totalCount,
                        (System.currentTimeMillis() - start) / 1000);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", derivedTableInterface.toString());
            }
        } catch (Exception e) {
            log.error("Exporting derived table exception " + derivedTableInterface.toString(), e);
        }
        return totalCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportShardTable(int targetHeight, int batchLimit) {
        int processedCount = 0;
        int totalCount = 0;
        String tableName = "shard";
        // prepare connection + statement + writer
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "select * from SHARD where shard_id > ? and shard_id < ? limit ?");
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, excludeColumnNamesInDerivedTables, "SHARD_ID");
             ) {
            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes 'comma' around values in csv            // select Min, Max DbId + rows count            // select Min, Max DbId + rows count
            MinMaxDbId minMaxDbId = shardDaoJdbc.getMinMaxId(dataSource, targetHeight);
            log.debug("Table = {}, Min/Max = {} at height = {}", tableName, minMaxDbId, targetHeight);

            // process non empty tables only
            if (minMaxDbId.getCount() > 0) {
                do { // do exporting into csv with pagination
                    processedCount = csvWriter.append(tableName,
                            shardDaoJdbc.getRangeByDbId(con, pstmt, minMaxDbId, batchLimit), minMaxDbId );
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {}", tableName, totalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", tableName);
            }
        } catch (Exception e) {
            log.error("Exporting derived table exception " + tableName, e);
        }
        return totalCount;
    }

}
