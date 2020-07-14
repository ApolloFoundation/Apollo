/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@Slf4j
public class DbTableWrapper<T extends DerivedEntity> implements EntityDbTableInterface<T> {

    protected EntityDbTableInterface<T> table;

    public DbTableWrapper(EntityDbTableInterface<T> table) {
        this.table = Objects.requireNonNull(table, "Table is NULL.");
    }

    @Override
    public void insert(T entity) {
        table.insert(entity);
    }

    @Override
    public int rollback(final int height) {
        return table.rollback(height);
    }

    @Override
    public boolean deleteAtHeight(T t, int height) {
        return table.deleteAtHeight(t, height);
    }

    @Override
    public void truncate() {
        table.truncate();
    }

    @Override
    public void save(Connection con, T entity) throws SQLException {
        table.save(con, entity);
    }

    @Override
    public T get(Connection con, PreparedStatement pstmt, boolean cache) throws SQLException {
        return table.get(con, pstmt, cache);
    }

    @Override
    public DbIterator<T> getManyBy(DbClause dbClause, int from, int to) {
        return table.getManyBy(dbClause, from, to);
    }

    @Override
    public DbIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort) {
        return table.getManyBy(dbClause, from, to, sort);
    }

    @Override
    public DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to) {
        return table.getManyBy(dbClause, height, from, to);
    }

    @Override
    public DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort) {
        return table.getManyBy(dbClause, height, from, to, sort);
    }

    @Override
    public DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache) {
        return table.getManyBy(con, pstmt, cache);
    }

    @Override
    public DbIterator<T> search(String query, DbClause dbClause, int from, int to) {
        return table.search(query, dbClause, from, to);
    }

    @Override
    public DbIterator<T> search(String query, DbClause dbClause, int from, int to, String sort) {
        return table.search(query, dbClause, from, to, sort);
    }

    @Override
    public String defaultSort() {
        return table.defaultSort();
    }

    @Override
    public T get(DbKey dbKey) {
        return table.get(dbKey);
    }

    @Override
    public T get(DbKey dbKey, boolean createDbKey) {
        return table.get(dbKey, createDbKey);
    }

    @Override
    public T get(DbKey dbKey, int height) {
        return table.get(dbKey, height);
    }

    @Override
    public T getBy(DbClause dbClause) {
        return table.getBy(dbClause);
    }

    @Override
    public int getCount() {
        return table.getCount();
    }

    @Override
    public int getCount(DbClause dbClause) {
        return table.getCount(dbClause);
    }

    @Override
    public int getCount(DbClause dbClause, int height) {
        return table.getCount(dbClause, height);
    }

    @Override
    public int getRowCount() {
        return table.getRowCount();
    }

    @Override
    public int getCount(PreparedStatement pstmt) throws SQLException {
        return table.getCount(pstmt);
    }

    @Override
    public boolean isMultiversion() {
        return table.isMultiversion();
    }

    @Override
    public String getName() {
        return table.getName();
    }

    @Override
    public String getFullTextSearchColumns() {
        return table.getFullTextSearchColumns();
    }

    @Override
    public DbIterator<T> getAll(int from, int to) {
        return table.getAll(from, to);
    }

    @Override
    public DbIterator<T> getAll(int from, int to, String sort) {
        return table.getAll(from, to, sort);
    }

    @Override
    public void trim(int height) {
        table.trim(height);
    }

    @Override
    public void trim(int height, boolean isSharding) {
        table.trim(height, isSharding);
    }

    @Override
    public DerivedTableData<T> getAllByDbId(long from, int limit, long dbIdLimit) throws SQLException {
        return table.getAllByDbId(from, limit, dbIdLimit);
    }

    @Override
    public ResultSet getRangeByDbId(Connection con, PreparedStatement pstmt, MinMaxValue minMaxValue, int limit) throws SQLException {
        return table.getRangeByDbId(con, pstmt, minMaxValue, limit);
    }

    @Override
    public MinMaxValue getMinMaxValue(int height) {
        return table.getMinMaxValue(height);
    }

    @Override
    public boolean supportDelete() {
        return table.supportDelete();
    }

    @Override
    public void prune(int time) {
        table.prune(time);
    }

    @Override
    public boolean isScanSafe() {
        return table.isScanSafe();
    }
}
