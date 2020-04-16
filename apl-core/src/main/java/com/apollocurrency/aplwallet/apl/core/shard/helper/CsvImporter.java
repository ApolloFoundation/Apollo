/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Component's interface for importing table data into Db from CSV file.
 * Configuration parameters are supplied in implementation Constructor.
 *
 * @author yuriy.larin
 */
public interface CsvImporter {

    /**
     * Return path to data export/import folder
     *
     * @return path to folder
     */
    Path getDataExportPath();

    /**
     * Import one specified table and returns number of imported rows
     * The CSV file should exist in folder specified by implementation component.
     * If file was not found it's skipped from processing
     *
     * @param tableName   table to import from csv into database
     * @param batchLimit  rows in batch to process
     * @param cleanTarget true if we want to cleanup target table
     * @return imported quantity
     */
    long importCsv(String tableName, int batchLimit, boolean cleanTarget) throws Exception;

    /**
     * Import one specified table and returns number of imported rows
     * The CSV file should exist in folder specified by implementation component.
     * If file was not found it's skipped from processing
     *
     * @param tableName   table to import from csv into database
     * @param batchLimit  rows in batch to process
     * @param cleanTarget true if we want to cleanup target table
     * @param rowDataHook function, which will be triggered on each imported row. Row represented by map of column name -> value.
     * @return imported quantity
     */
    long importCsvWithRowHook(String tableName, int batchLimit, boolean cleanTarget, Consumer<Map<String, Object>> rowDataHook) throws Exception;

    /**
     * Import csv file for specified table into db. During import, default values will be also set into each executed statement
     *
     * @param tableName     db table name to import
     * @param batchLimit    number of executed statement to commit
     * @param cleanTarget   clean target db table or not
     * @param defaultParams map of column name->value to set for each statement
     * @return number of imported rows
     */
    long importCsvWithDefaultParams(String tableName, int batchLimit, boolean cleanTarget, Map<String, Object> defaultParams) throws Exception;
}
