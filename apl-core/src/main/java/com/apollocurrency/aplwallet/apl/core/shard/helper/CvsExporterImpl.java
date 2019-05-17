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
import java.sql.SQLException;
import java.util.HashSet;
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
public class CvsExporterImpl implements CvsExporter {
    private static final Logger log = getLogger(CvsExporterImpl.class);

    private Path dataExportPath;
    private DatabaseManager databaseManager;
    private ShardDaoJdbc shardDaoJdbc;
    private Set<String> excludeColumnNamesInDerivedTables;
    private CsvWriter csvWriter;

    @Inject
    public CvsExporterImpl(@Named("dataExportDir") Path dataExportPath, DatabaseManager databaseManager,
                           ShardDaoJdbc shardDaoJdbc) {
        this.dataExportPath = Objects.requireNonNull(dataExportPath, "data export Path is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.shardDaoJdbc = Objects.requireNonNull(shardDaoJdbc, "shardDaoJdbc is NULL");
        excludeColumnNamesInDerivedTables = new HashSet<>(1) {{
          add("DB_ID");
        }};
        csvWriter = new CsvWriterImpl(dataExportPath, excludeColumnNamesInDerivedTables, "DB_ID");
        csvWriter.setOptions("fieldDelimiter="); // do not put "" around column/values
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportDerivedTable(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit) {
        Objects.requireNonNull(derivedTableInterface, "derivedTableInterface is NULL");
        if (csvWriter.getDefaultPaginationColumnName() != null
                && !csvWriter.getDefaultPaginationColumnName().equalsIgnoreCase("DB_ID")) {
            csvWriter.setDefaultPaginationColumnName("DB_ID");
        }
        int processedCount = 0;
        int totalCount = 0;
        // prepare connection + statement
        try (Connection con = this.databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "select * from " + derivedTableInterface.toString() + " where db_id > ? and db_id < ? limit ?")) {
            // select Min, Max DbId + rows count
            MinMaxDbId minMaxDbId = derivedTableInterface.getMinMaxDbId(targetHeight);
            log.debug("Table = {}, Min/Max = {} at height = {}", derivedTableInterface.toString(), minMaxDbId, targetHeight);

            // process non empty tables only
            if (minMaxDbId.getCount() > 0) {
                do { // do exporting into csv with pagination
                    processedCount = csvWriter.append(derivedTableInterface.toString(),
                            derivedTableInterface.getRangeByDbId(con, pstmt, minMaxDbId, batchLimit), minMaxDbId );
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {}", derivedTableInterface.toString(), totalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", derivedTableInterface.toString());
            }
        } catch (SQLException e) {
            log.error("Exporting derived table exception " + derivedTableInterface.toString(), e);
        } finally {
            if (csvWriter != null) {
                csvWriter.close(); // close CSV file
            }
        }
        return totalCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportShardTable(int targetHeight, int batchLimit) {
        if (csvWriter.getDefaultPaginationColumnName() != null
                && !csvWriter.getDefaultPaginationColumnName().equalsIgnoreCase("SHARD_ID")) {
            csvWriter.setDefaultPaginationColumnName("SHARD_ID");
        }
        int processedCount = 0;
        int totalCount = 0;
        String tableName = "shard";
        // prepare connection + statement
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "select * from SHARD where shard_id > ? and shard_id < ? limit ?")) {
            // select Min, Max DbId + rows count
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
        } catch (SQLException e) {
            log.error("Exporting derived table exception " + tableName, e);
        } finally {
            if (csvWriter != null) {
                csvWriter.close(); // close CSV file
            }
        }
        return totalCount;
    }

}
