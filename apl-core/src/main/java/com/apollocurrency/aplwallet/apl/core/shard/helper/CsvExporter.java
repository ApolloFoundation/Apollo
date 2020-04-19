/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * Component's interface for exporting table data from one Db into CSV
 * Configuration parameters are supplied in implementation Constructor.
 *
 * @author yuriy.larin
 */
public interface CsvExporter {

    /**
     * Return path to CSV data export folder
     *
     * @return path to folder
     */
    Path getDataExportPath();

    /**
     * Exports one specified table and returns number of exported rows
     * The CSV file put into folder specified by implementation component
     *
     * @param tableInterface table to export
     * @param targetHeight   target blockchain height
     * @param batchLimit     rows in batch to process
     * @return exported quantity
     */
    long exportDerivedTable(DerivedTableInterface tableInterface, int targetHeight, int batchLimit);

    long exportPrunableDerivedTable(PrunableDbTable derivedTableInterface, int targetHeight, int currentTime, int batchLimit);

    /**
     * Exports one SHARD table and returns number of exported rows
     * The CSV file put into folder specified by implementation component.
     *
     * @param targetHeight target blockchain height
     * @param batchLimit   rows in batch to process
     * @return exported quantity
     */
    long exportShardTable(int targetHeight, int batchLimit);

    /**
     * Perform 'shard' table export as {@link CsvExporter#exportShardTable(int, int)} but
     * leave last shard entry without zip hashes as it performed during sharding process
     * to meet shard archive requirements
     *
     * @param targetHeight target blockchain height
     * @param batchLimit   rows in batch to process
     * @return exported quantity
     */
    long exportShardTableIgnoringLastZipHashes(int targetHeight, int batchLimit);

    long exportTransactionIndex(int targetHeight, int batchLimit);

    long exportBlockIndex(int targetHeight, int batchLimit);

    /**
     * Export derived table and returns number of exported rows
     * Will exclude columns specified in excludeColumns parameter
     *
     * @param derivedTableInterface table to export
     * @param targetHeight          target blockchain height
     * @param batchLimit            rows in batch to commit
     * @param excludedColumns       set of columns to exclude during export
     * @return number of exported rows
     */
    long exportDerivedTable(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit, Set<String> excludedColumns);


    /**
     * Export derived table entries ordered by custom sort. Custom sort may include few columns. Note, that it is a slow method for export, because it uses
     * LIMIT + OFFSET pagination model, so that should be used when really necessary
     * Will exclude columns specified in excludeColumns parameter
     *
     * @param derivedTableInterface table to export
     * @param targetHeight          target blockchain height
     * @param batchLimit            rows in batch to commit
     * @param excludedColumns       set of columns to exclude during export
     * @param sortColumn            sort expression to order rows
     * @return number of exported rows
     */
    long exportDerivedTableCustomSort(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit, Set<String> excludedColumns, String sortColumn);

    /**
     * Export transactions specified by db_id list and block height then return number of exported transactions
     *
     * @param dbIds  collection of transaction db_ids
     * @param height transactions for snapshot block height
     * @return number of exported transactions
     */
    long exportTransactions(Collection<Long> dbIds, int height);

    /**
     * Export block with transactions at given height
     *
     * @param height height of block to export
     * @return number of exported transactions + 1
     * @throws IllegalStateException when block with given height was not found or db has several blocks at the same height
     */
    long exportBlock(int height) throws IllegalStateException;
}
