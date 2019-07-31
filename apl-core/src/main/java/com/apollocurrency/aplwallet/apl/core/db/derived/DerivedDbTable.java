/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.util.StringValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.CDI;

public abstract class DerivedDbTable<T> implements DerivedTableInterface<T> {

    private FullTextConfig fullTextConfig;
    private DerivedTablesRegistry derivedDbTablesRegistry;

    public String getTableName() {
        return table;
    }

    protected final String table;
    protected DatabaseManager databaseManager;

    //TODO: fix injects and remove
    private void lookupCdi(){
        if(fullTextConfig==null){
            fullTextConfig =  CDI.current().select(FullTextConfig.class).get();
        }
        if(derivedDbTablesRegistry==null){
            derivedDbTablesRegistry = CDI.current().select(DerivedTablesRegistry.class).get();
        }
    }

    // We should find better place for table init
    protected DerivedDbTable(String table, boolean init) { // for CDI beans setUp 'false'
        StringValidator.requireNonBlank(table, "Table name");
        this.table = table;
        databaseManager = CDI.current().select(DatabaseManager.class).get();
        if (init) {
            init();
        }
    }

    protected DerivedDbTable(String table) {
        this(table, true);
    }

    @Override
    public void trim(int height) {}

    @PostConstruct
    public void init() {
        lookupCdi();
        derivedDbTablesRegistry.registerDerivedTable(this);
        fullTextConfig.registerTable(table);
    }

    @Override
    public void rollback(int height) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + " WHERE height > ?")) {
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public boolean delete(T t) {
        throw new UnsupportedOperationException("Delete is not supported");
    }

    @Override
    public void insert(T t) {
        throw new UnsupportedOperationException("Insert is not supported");
    }

    @Override
    public void createSearchIndex(Connection con) throws SQLException {

    }

    @Override
    public void truncate() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("TRUNCATE TABLE " + table);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    public  DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DerivedTableData<T> getAllByDbId(long from, int limit, long dbIdLimit) throws SQLException {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("select * from " + table + " where db_id >= ? and db_id < ? limit ?")) {
            pstmt.setLong(1, from);
            pstmt.setLong(2, dbIdLimit);
            pstmt.setLong(3, limit);
            List<T> values = new ArrayList<>();
            long dbId = -1;
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()){
                    values.add(load(con, rs, null));
                    dbId = rs.getLong("db_id");
                }
            }

            return new DerivedTableData<>(values, dbId);
        }
    }

    protected abstract T load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet getRangeByDbId(Connection con, PreparedStatement pstmt,
                                    MinMaxDbId minMaxDbId, int limit) throws SQLException {
        Objects.requireNonNull(con, "connnection is NULL");
        Objects.requireNonNull(pstmt, "prepared statement is NULL");
        Objects.requireNonNull(minMaxDbId, "minMaxDbId is NULL");
        try {
            pstmt.setLong(1, minMaxDbId.getMinDbId());
            pstmt.setLong(2, minMaxDbId.getMaxDbId());
            pstmt.setLong(3, limit);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MinMaxDbId getMinMaxDbId(int height){
        // select MIN and MAX dbId values in one query
        String selectMinSql = String.format("SELECT IFNULL(min(DB_ID), 0) as min_DB_ID, " +
                "IFNULL(max(DB_ID), 0) as max_DB_ID, IFNULL(count(*), 0) as count, max(height) as max_height from %s where HEIGHT <= ?",  table);

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(selectMinSql)) {
            pstmt.setInt(1, height);
            return getMinMaxDbId(pstmt);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected MinMaxDbId getMinMaxDbId(PreparedStatement pstmt) throws SQLException {
        MinMaxDbId result = null;
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                long dbIdMin = rs.getLong("min_db_id");
                long dbIdMax = rs.getLong("max_db_id");
                long rowCount = rs.getLong("count");
                int height = rs.getInt("max_height");
                // pagination is exclusive for upper + lower bounds
                result = new MinMaxDbId(
                        dbIdMin - 1, // plus/minus one in Max/Min value
                        dbIdMax + 1,
                        rowCount,
                        height
                );
            }
        }
        return result;
    }

    @Override
    public boolean isScanSafe() {
        return true; // by default derived table can be safely rolled back to any height without block popOff
    }
    @Override
    public final String toString() {
        return getName();
    }

    @Override
    public String getName() {
        return table;
    }
}
