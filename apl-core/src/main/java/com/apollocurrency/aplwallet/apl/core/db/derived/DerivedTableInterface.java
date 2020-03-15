/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Common derived interface functions. It supports rollback, truncate, trim.
 *
 * @author yuriy.larin
 */
public interface DerivedTableInterface<T> {

    /**
     * Remove all records from the derived table above the height.
     * @param height the height
     * @return the number of removed records
     */
    int rollback(int height);

    /**
     * @return true, when this table is not a part of blockchain core data and can be reverted and re-populated
     * during blockchain scan without downloading data from peers, othervise return false, which means that this table
     * cannot be rolled back without blocks popOff
     */
    boolean isScanSafe();

    void truncate();

    void trim(int height);

    /**
     * Trim process can be different when it runs on usual way or in case sharding process
     * @param height trim height
     * @param isSharding true when called in sharding process, false otherwise (do the same as usual trim)
     */
    void trim(int height, boolean isSharding);

    void prune(int time);

    void insert(T t);

    /**
     * Method is used by unit tests mainly
     * @param from bottom column value (id or similar)
     * @param limit top column value (id or similar)
     * @param dbIdLimit batch value
     * @return table rows with lastDbId inside
     * @throws SQLException
     */
    DerivedTableData<T> getAllByDbId(long from, int limit, long dbIdLimit) throws SQLException;

    boolean deleteAtHeight(T t, int height);

    /**
     * Retrieve sql result set partial table's data for later processing with pagination on current table
     *
     * @param con sql connection to use for sql statement
     * @param pstmt select sql to execute for selecting with pagination
     * @param minMaxValue object to keep track on latest ID during pagination
     * @param limit batch pagination limit
     * @return sql result set
     * @throws SQLException
     */
    ResultSet getRangeByDbId(Connection con, PreparedStatement pstmt,
                             MinMaxValue minMaxValue, int limit) throws SQLException;

    /**
     * Request min, max DB_ID, rows count on current table for later use by retrieving logic
     *
     * @param height target blockchain height
     * @return object with internal values for min, max DB_ID and count of rows
     * @throws SQLException
     */
    MinMaxValue getMinMaxValue(int height);

    /**
     * @return table db name
     */
    String getName();

    /**
     * Gets null as full text search columns only for those tables that do not have
     * the EntityDbTable ancestor.
     *
     * @return null
     */
    default String getFullTextSearchColumns() {
        return null;
    }
}
