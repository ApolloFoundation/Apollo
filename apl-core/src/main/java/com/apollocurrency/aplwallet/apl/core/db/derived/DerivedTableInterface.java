package com.apollocurrency.aplwallet.apl.core.db.derived;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Common derived interface functions. It supports rollback, truncate, trim.
 *
 * @author yuriy.larin
 */
public interface DerivedTableInterface<T> {

    void rollback(int height);

    void truncate();

    void trim(int height);

    void createSearchIndex(Connection con) throws SQLException;

    void insert(T t);

    DerivedTableData<T> getAllByDbId(long from, int limit, long dbIdLimit) throws SQLException;

    boolean delete(T t);

}
