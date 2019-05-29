/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;

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
     * @param targetHeight target blockchain height
     * @param batchLimit rows in batch to process
     * @return exported quantity
     */
    long exportDerivedTable(DerivedTableInterface tableInterface, int targetHeight, int batchLimit);

    /**
     * Exports one SHARD table and returns number of exported rows
     * The CSV file put into folder specified by implementation component.
     *
     * @param targetHeight target blockchain height
     * @param batchLimit rows in batch to process
     * @return exported quantity
     */
    long exportShardTable(int targetHeight, int batchLimit);

}
