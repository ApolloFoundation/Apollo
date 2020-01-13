/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface EntityDbTableInterface<T> extends DerivedTableInterface<T> {

    void save(Connection con, T entity) throws SQLException;

    String defaultSort();

    void clearCache();

    void checkAvailable(int height);

    @Deprecated
    T newEntity(DbKey dbKey);

    T get(DbKey dbKey);

    T get(DbKey dbKey, boolean cache);

    T get(DbKey dbKey, int height);

    T getBy(DbClause dbClause);

    T getBy(DbClause dbClause, int height);

    T get(Connection con, PreparedStatement pstmt, boolean cache) throws SQLException;

    DbIterator<T> getManyBy(DbClause dbClause, int from, int to);

    DbIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort);

    DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to);

    DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort);

    DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache);

    DbIterator<T> search(String query, DbClause dbClause, int from, int to);

    DbIterator<T> search(String query, DbClause dbClause, int from, int to, String sort);

    DbIterator<T> getAll(int from, int to);

    DbIterator<T> getAll(int from, int to, String sort);

    DbIterator<T> getAll(int height, int from, int to);

    DbIterator<T> getAll(int height, int from, int to, String sort);

    int getCount();

    int getCount(DbClause dbClause);

    int getCount(DbClause dbClause, int height);

    int getRowCount();

    int getCount(PreparedStatement pstmt) throws SQLException;

    boolean doesNotExceed(int height);
}
