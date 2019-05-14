/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc;

import java.sql.SQLException;

/**
 * This interface is for classes that create rows on demand.
 * It is used together with SimpleResultSet to create a dynamic result set.
 */
public interface SimpleRowSource {

    /**
     * Get the next row. Must return null if no more rows are available.
     *
     * @return the row or null
     */
    Object[] readRow() throws SQLException;

    /**
     * Close the row source.
     */
    void close();

    /**
     * Reset the position (before the first row).
     *
     * @throws SQLException if this operation is not supported
     */
    void reset() throws SQLException;
}
