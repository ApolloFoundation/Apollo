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

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class EntityDbTable<T> extends BasicDbTable<T> implements EntityDbTableInterface<T>{
    private static final Logger log = getLogger(EntityDbTable.class);

    private final String defaultSort;
    private final String fullTextSearchColumns;
    private  Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private FullTextSearchService fullText;

    protected EntityDbTable(String table, KeyFactory<T> dbKeyFactory) {
        this(table, dbKeyFactory, false, null);
    }

    protected EntityDbTable(String table, KeyFactory<T> dbKeyFactory, String fullTextSearchColumns) {
        this(table, dbKeyFactory, false, fullTextSearchColumns);
    }

    public EntityDbTable(String table, KeyFactory<T> dbKeyFactory, boolean init) {
        this(table, dbKeyFactory, false, null, init);
    }

    public EntityDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns, boolean init) {
        super(table, dbKeyFactory, multiversion, init);
        this.defaultSort = " ORDER BY " + (multiversion ? dbKeyFactory.getPKColumns() : " height DESC, db_id DESC ");
        this.fullTextSearchColumns = fullTextSearchColumns;
    }

    EntityDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns) {
        this(table, dbKeyFactory, multiversion, fullTextSearchColumns, true);
    }

    public abstract void save(Connection con, T entity) throws SQLException;

    @Override
    public String defaultSort() {
        return defaultSort;
    }

    public void checkAvailable(int height) {
        if (multiversion) {
            int minRollBackHeight = lookupBlockchainProcessor().getMinRollbackHeight();
            if (height < minRollBackHeight) {
                throw new IllegalArgumentException("Historical data as of height " + height + " not available.");
            }
        }

        if (height > lookupBlockchain().getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + blockchain.getHeight());
        }
    }

    //TODO: Fix injection and remove
    protected BlockchainProcessor lookupBlockchainProcessor(){
        if (blockchainProcessor == null){
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        }
        return blockchainProcessor;
    }

    //TODO: Fix injection and remove
    protected Blockchain lookupBlockchain(){
        if (blockchain == null){
            blockchain = CDI.current().select(BlockchainImpl.class).get();
        }
        return blockchain;
    }

    @Override
    public T get(DbKey dbKey) {
        return get(dbKey, true);
    }

    @Override
    public T get(DbKey dbKey, boolean createDbKey) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + keyFactory.getPKClause()
             + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            dbKey.setPK(pstmt);
            return get(con, pstmt, createDbKey);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public T get(DbKey dbKey, int height) {
        if (height < 0 || doesNotExceed(height)) {
            return get(dbKey);
        }
        checkAvailable(height);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + keyFactory.getPKClause()
                     + " AND height <= ?" + (multiversion ? " AND (latest = TRUE OR EXISTS ("
                     + "SELECT 1 FROM " + table + keyFactory.getPKClause() + " AND height > ?)) ORDER BY height DESC LIMIT 1" : ""))) {
            int i = dbKey.setPK(pstmt);
            pstmt.setInt(i, height);
            if (multiversion) {
                i = dbKey.setPK(pstmt, ++i);
                pstmt.setInt(i, height);
            }
            return get(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final T getBy(DbClause dbClause) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table
                     + " WHERE " + dbClause.getClause() + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            dbClause.set(pstmt, 1);
            return get(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final T getBy(DbClause dbClause, int height) {
        if (height < 0 || doesNotExceed(height)) {
            return getBy(dbClause);
        }
        checkAvailable(height);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + " AS a WHERE " + dbClause.getClause()
                     + " AND height <= ?" + (multiversion ? " AND (latest = TRUE OR EXISTS ("
                     + "SELECT 1 FROM " + table + " AS b WHERE " + keyFactory.getSelfJoinClause()
                     + " AND b.height > ?)) ORDER BY height DESC LIMIT 1" : ""))) {
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            pstmt.setInt(i, height);
            if (multiversion) {
                pstmt.setInt(++i, height);
            }
            return get(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public T get(Connection con, PreparedStatement pstmt, boolean createDbKey) throws SQLException {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            DbKey dbKey = createDbKey && dataSource.isInTransaction()
                    ? keyFactory.newKey(rs)
                    : null;
            T t = load(con, rs, dbKey);
            if (rs.next() && dbKey!=null) {
              log.debug("Multiple records found. Table: {} Key: {}", table, dbKey.toString());
              throw new RuntimeException("Multiple records found. Table: "+table+" Key: "+dbKey.toString());
            }
            return t;
        }
    }

    @Override
    public final DbIterator<T> getManyBy(DbClause dbClause, int from, int to) {
        return getManyBy(dbClause, from, to, defaultSort());
    }

    @Override
    public final DbIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table
                    + " WHERE " + dbClause.getClause() + (multiversion ? " AND latest = TRUE " : " ") + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            i = DbUtils.setLimits(i, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to) {
        return getManyBy(dbClause, height, from, to, defaultSort());
    }

    @Override
    public final DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort) {
        if (height < 0 || doesNotExceed(height)) {
            return getManyBy(dbClause, from, to, sort);
        }
        checkAvailable(height);
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + " AS a WHERE " + dbClause.getClause()
                    + " AND a.height <= ?" + (multiversion ? " AND (a.latest = TRUE OR (a.latest = FALSE "
                    + " AND EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + keyFactory.getSelfJoinClause() + " AND b.height > ?) "
                    + " AND NOT EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + keyFactory.getSelfJoinClause()
                    + " AND b.height <= ? AND b.height > a.height))) "
                    : " ") + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            pstmt.setInt(i, height);
            if (multiversion) {
                pstmt.setInt(++i, height);
                pstmt.setInt(++i, height);
            }
            i = DbUtils.setLimits(++i, pstmt, from, to);
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        final boolean doCache = cache && dataSource.isInTransaction();
        return new DbIterator<>(con, pstmt, (connection, rs) -> {
            T t = null;
            DbKey dbKey = null;
            if (doCache) {
                dbKey = keyFactory.newKey(rs);
            }
            if (t == null) {
                t = (T) load(connection, rs, dbKey);
            }
            return t;
        });
    }

    @Override
    public final DbIterator<T> search(String query, DbClause dbClause, int from, int to) {
        return search(query, dbClause, from, to, " ORDER BY ft.score DESC ");
    }

    @Override
    public final DbIterator<T> search(String query, DbClause dbClause, int from, int to, String sort) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement("SELECT " + table + ".*, ft.score FROM " + table +
                    ", ftl_search('PUBLIC', '" + table + "', ?, 2147483647, 0) ft "
                    + " WHERE " + table + ".db_id = ft.keys[1] "
                    + (multiversion ? " AND " + table + ".latest = TRUE " : " ")
                    + " AND " + dbClause.getClause() + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setString(++i, query);
            i = dbClause.set(pstmt, ++i);
            i = DbUtils.setLimits(i, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<T> getAll(int from, int to) {
        return getAll(from, to, defaultSort());
    }

    @Override
    public final DbIterator<T> getAll(int from, int to, String sort) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table
                     + (multiversion ? " WHERE latest = TRUE " : " ") + sort
                    + DbUtils.limitsClause(from, to));
            DbUtils.setLimits(1, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final DbIterator<T> getAll(int height, int from, int to) {
        return getAll(height, from, to, defaultSort());
    }

    @Override
    public final DbIterator<T> getAll(int height, int from, int to, String sort) {
        if (height < 0 || doesNotExceed(height)) {
            return getAll(from, to, sort);
        }
        checkAvailable(height);
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + " AS a WHERE height <= ?"
                    + (multiversion ? " AND (latest = TRUE OR (latest = FALSE "
                    + "AND EXISTS (SELECT 1 FROM " + table + " AS b WHERE b.height > ? AND " + keyFactory.getSelfJoinClause()
                    + ") AND NOT EXISTS (SELECT 1 FROM " + table + " AS b WHERE b.height <= ? AND " + keyFactory.getSelfJoinClause()
                    + " AND b.height > a.height))) " : " ") + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setInt(++i, height);
            if (multiversion) {
                pstmt.setInt(++i, height);
                pstmt.setInt(++i, height);
            }
            i = DbUtils.setLimits(++i, pstmt, from, to);
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getCount() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table
                     + (multiversion ? " WHERE latest = TRUE" : ""))) {
            return getCount(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final int getCount(DbClause dbClause) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table
                     + " WHERE " + dbClause.getClause() + (multiversion ? " AND latest = TRUE" : ""))) {
            dbClause.set(pstmt, 1);
            return getCount(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final int getCount(DbClause dbClause, int height) {
        if (height < 0 || doesNotExceed(height)) {
            return getCount(dbClause);
        }
        checkAvailable(height);
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table + " AS a WHERE " + dbClause.getClause()
                    + "AND a.height <= ?" + (multiversion ? " AND (a.latest = TRUE OR (a.latest = FALSE "
                    + "AND EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + keyFactory.getSelfJoinClause() + " AND b.height > ?) "
                    + "AND NOT EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + keyFactory.getSelfJoinClause()
                    + " AND b.height <= ? AND b.height > a.height))) "
                    : " "));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            pstmt.setInt(i, height);
            if (multiversion) {
                pstmt.setInt(++i, height);
                pstmt.setInt(++i, height);
            }
            return getCount(pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }finally{
            DbUtils.close(con);
        }
    }

    @Override
    public final int getRowCount() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table)) {
            return getCount(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getCount(PreparedStatement pstmt) throws SQLException {
        try (ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

//    protected abstract void save(Connection con, T entity) throws SQLException;

    public void insert(T t) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = keyFactory.newKey(t);
        if (dbKey == null) {
            throw new RuntimeException("DbKey not set");
        }
        try (Connection con = dataSource.getConnection()) {
            if (multiversion) {
                try (
                        @DatabaseSpecificDml(DmlMarker.UPDATE_WITH_LIMIT)
                        PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                        + " SET latest = FALSE " + keyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")
                ) {
                    dbKey.setPK(pstmt);
                    pstmt.executeUpdate();
                }
            }
            save(con, t);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @Override
    public final void createSearchIndex(Connection con) throws SQLException {
        if (fullTextSearchColumns != null) {
            log.debug("Creating search index on " + table + " (" + fullTextSearchColumns + ")");
            if (fullText == null) {
                fullText = CDI.current().select(FullTextSearchService.class).get();
            }
            fullText.createIndex(con, "PUBLIC", table.toUpperCase(), fullTextSearchColumns.toUpperCase());
        }
    }

    @Override
    public boolean doesNotExceed(int height) {
        if (blockchain == null) blockchain = CDI.current().select(BlockchainImpl.class).get();
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        return blockchain.getHeight() <= height;
    }

}
