/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;

/**
 * Shard retrieving interface used for retrieving data by pagination and exporting into CSV.
 */
public interface ShardDaoJdbc {

    /**
     * Request min, max DB_ID, rows count on current table for later use by retrieving logic
     *
     * @param sourceDataSource datasource for getting connection
     * @param height target blockchain height
     * @return object with internal values for min, max DB_ID and count of rows
     * @throws SQLException
     */
    MinMaxDbId getMinMaxId(TransactionalDataSource sourceDataSource, long height) throws SQLException;

    /**
     * Retrieve sql result set partial table's data for later processing with pagination on current table
     *
     * @param con sql connection to use for sql statement
     * @param pstmt select sql to execute for selecting with pagination
     * @param minMaxDbId object to keep track on latest ID during pagination
     * @param limit batch pagination limit
     * @return sql result set
     * @throws SQLException
     */
    ResultSet getRangeByDbId(Connection con, PreparedStatement pstmt,
                             MinMaxDbId minMaxDbId, int limit) throws SQLException;

}