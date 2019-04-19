package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Common derived interface functions. It supports rollback, truncate, trim.
 *
 * @author yuriy.larin
 */
public interface DerivedTableInterface<T> {

    void rollback(int height);

    void truncate();

    void trim(int height, TransactionalDataSource dataSource);

    default void trim(int height) {}

    default void createSearchIndex(Connection con) throws SQLException {}

    default boolean isPersistent() {
        return false;
    }

    default T load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {return null;}

    void save(Connection con, T entity) throws SQLException;

    default void insert(T t) {
        throw new UnsupportedOperationException("unsupported insert");
    }

    default DerivedTableData<T> getAllByDbId(long from, int limit, long dbIdLimit) throws SQLException {
        throw new UnsupportedOperationException("GetAll is not supported");
    }

    default boolean delete(T t) {
        throw new UnsupportedOperationException("Delete is not supported");
    }

}
