/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

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

    void rollback(int height);

    void truncate();

    void trim(int height, TransactionalDataSource dataSource);

    default void trim(int height) {}

    default void createSearchIndex(Connection con) throws SQLException {}

    default boolean isPersistent() {
        return false;
    }

    default T load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {return null;}

    default void insert(T t) {
        throw new UnsupportedOperationException("unsupported insert");
    }

    default DerivedTableData<T> getAllByDbId(MinMaxDbId minMaxDbId, int limit) throws SQLException {
        throw new UnsupportedOperationException("GetAll is not supported");
    }

    default ResultSet getRangeByDbId(Connection con, PreparedStatement pstmt,
                                     MinMaxDbId minMaxDbId, int limit) throws SQLException {
        throw new UnsupportedOperationException("GetRange is not supported");
    }

    default MinMaxDbId getMinMaxDbId(int height) throws SQLException {
        return new MinMaxDbId();
    }

    default boolean delete(T t) {
        throw new UnsupportedOperationException("Delete is not supported");
    }

}
