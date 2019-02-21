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

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;

public abstract class EntityDbTable<T> extends DerivedDbTable {
    private static final Logger log = getLogger(EntityDbTable.class);

    public static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();

    private final boolean multiversion;
    protected final KeyFactory<T> dbKeyFactory;
    private final String defaultSort;
    private final String fullTextSearchColumns;
    private static Blockchain blockchain;
    private static BlockchainProcessor blockchainProcessor;
    private static FullTextSearchService fullText = CDI.current().select(FullTextSearchService.class).get();

    protected EntityDbTable(String table, KeyFactory<T> dbKeyFactory) {
        this(table, dbKeyFactory, false, null);
    }

    protected EntityDbTable(String table, KeyFactory<T> dbKeyFactory, String fullTextSearchColumns) {
        this(table, dbKeyFactory, false, fullTextSearchColumns);
    }

    EntityDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns) {
        super(table);
        this.dbKeyFactory = dbKeyFactory;
        this.multiversion = multiversion;
        this.defaultSort = " ORDER BY " + (multiversion ? dbKeyFactory.getPKColumns() : " height DESC, db_id DESC ");
        this.fullTextSearchColumns = fullTextSearchColumns;
    }

     protected EntityDbTable(String table, boolean multiversion, KeyFactory<T> dbKeyFactory) {
         super(table);
         this.multiversion = multiversion;
         this.dbKeyFactory = dbKeyFactory;
         this.defaultSort = "";
         this.fullTextSearchColumns = "";
    }

    protected abstract T load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException;

    protected abstract void save(Connection con, T t) throws SQLException;

    protected String defaultSort() {
        return defaultSort;
    }

    protected void clearCache() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        dataSource.clearCache(table);
    }

    public void checkAvailable(int height) {
        if (multiversion) {
            if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
            int minRollBackHeight = isPersistent() && blockchainProcessor.isScanning() ?
                    Math.max(blockchainProcessor.getInitialScanHeight() - propertiesHolder.MAX_ROLLBACK(), 0)
                    : blockchainProcessor.getMinRollbackHeight();
            if (height < minRollBackHeight) {
                throw new IllegalArgumentException("Historical data as of height " + height + " not available.");
            }
        }
        if (blockchain == null) blockchain = CDI.current().select(BlockchainImpl.class).get();
        if (height > blockchain.getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + blockchain.getHeight());
        }
    }

    public final T newEntity(DbKey dbKey) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        boolean cache = dataSource.isInTransaction();
        if (cache) {
            T t = (T) dataSource.getCache(table).get(dbKey);
            if (t != null) {
                return t;
            }
        }
        T t = dbKeyFactory.newEntity(dbKey);
        if (cache) {
            dataSource.getCache(table).put(dbKey, t);
        }
        return t;
    }

    public final T get(DbKey dbKey) {
        return get(dbKey, true);
    }

    public final T get(DbKey dbKey, boolean cache) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (cache && dataSource.isInTransaction()) {
            T t = (T) dataSource.getCache(table).get(dbKey);
            if (t != null) {
                return t;
            }
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + dbKeyFactory.getPKClause()
             + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            dbKey.setPK(pstmt);
            return get(con, pstmt, cache);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final T get(DbKey dbKey, int height) {
        if (height < 0 || doesNotExceed(height)) {
            return get(dbKey);
        }
        checkAvailable(height);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + dbKeyFactory.getPKClause()
                     + " AND height <= ?" + (multiversion ? " AND (latest = TRUE OR EXISTS ("
                     + "SELECT 1 FROM " + table + dbKeyFactory.getPKClause() + " AND height > ?)) ORDER BY height DESC LIMIT 1" : ""))) {
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

    public final T getBy(DbClause dbClause, int height) {
        if (height < 0 || doesNotExceed(height)) {
            return getBy(dbClause);
        }
        checkAvailable(height);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + " AS a WHERE " + dbClause.getClause()
                     + " AND height <= ?" + (multiversion ? " AND (latest = TRUE OR EXISTS ("
                     + "SELECT 1 FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause()
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

    private T get(Connection con, PreparedStatement pstmt, boolean cache) throws SQLException {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        final boolean doCache = cache && dataSource.isInTransaction();
        try (ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            T t = null;

            DbKey dbKey = null;
            if (doCache) {
                dbKey = dbKeyFactory.newKey(rs);                              
                t = (T) dataSource.getCache(table).get(dbKey);
            }
            if (t == null) {
                t = load(con, rs, dbKey);
                if (doCache) {
                    dataSource.getCache(table).put(dbKey, t);
                }
            }
            if (rs.next() && dbKey!=null) {
              log.debug("Multiple records found. Table: "+table+" Key: "+dbKey.toString());
              throw new RuntimeException("Multiple records found. Table: "+table+" Key: "+dbKey.toString());
            }
            return t;
        }
    }

    public final DbIterator<T> getManyBy(DbClause dbClause, int from, int to) {
        return getManyBy(dbClause, from, to, defaultSort());
    }

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

    public final DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to) {
        return getManyBy(dbClause, height, from, to, defaultSort());
    }

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
                    + "AND a.height <= ?" + (multiversion ? " AND (a.latest = TRUE OR (a.latest = FALSE "
                    + "AND EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause() + " AND b.height > ?) "
                    + "AND NOT EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause()
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

    public final DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        final boolean doCache = cache && dataSource.isInTransaction();
        return new DbIterator<>(con, pstmt, (connection, rs) -> {
            T t = null;
            DbKey dbKey = null;
            if (doCache) {
                dbKey = dbKeyFactory.newKey(rs);
                t = (T) dataSource.getCache(table).get(dbKey);
            }
            if (t == null) {
                t = load(connection, rs, dbKey);
                if (doCache) {
                    dataSource.getCache(table).put(dbKey, t);
                }
            }
            return t;
        });
    }

    public final DbIterator<T> search(String query, DbClause dbClause, int from, int to) {
        return search(query, dbClause, from, to, " ORDER BY ft.score DESC ");
    }

    public final DbIterator<T> search(String query, DbClause dbClause, int from, int to, String sort) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT " + table + ".*, ft.score FROM " + table +
                    ", ftl_search('PUBLIC', '" + table + "', ?, 2147483647, 0) ft "
                    + " WHERE " + table + ".db_id = ft.keys[0] "
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

    public final DbIterator<T> getAll(int from, int to) {
        return getAll(from, to, defaultSort());
    }

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

    public final DbIterator<T> getAll(int height, int from, int to) {
        return getAll(height, from, to, defaultSort());
    }

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
                    + "AND EXISTS (SELECT 1 FROM " + table + " AS b WHERE b.height > ? AND " + dbKeyFactory.getSelfJoinClause()
                    + ") AND NOT EXISTS (SELECT 1 FROM " + table + " AS b WHERE b.height <= ? AND " + dbKeyFactory.getSelfJoinClause()
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

    public final int getCount() {
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
                    + "AND EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause() + " AND b.height > ?) "
                    + "AND NOT EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause()
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
        }
    }

    public final int getRowCount() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table)) {
            return getCount(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private int getCount(PreparedStatement pstmt) throws SQLException {
        try (ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public final void insert(T t) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        if (dbKey == null) {
            throw new RuntimeException("DbKey not set");
        }
        T cachedT = (T) dataSource.getCache(table).get(dbKey);
        if (cachedT == null) {
            dataSource.getCache(table).put(dbKey, t);
        } else if (t != cachedT) { // not a bug
            log.debug("In cache : " + cachedT.toString() + ", inserting " + t.toString());
            throw new IllegalStateException("Different instance found in DatabaseManager cache, perhaps trying to save an object "
                    + "that was read outside the current transaction");
        }
        try (Connection con = dataSource.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                        + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
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
    public void rollback(int height) {
        if (multiversion) {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            VersionedEntityDbTable.rollback(dataSource, table, height, dbKeyFactory);
        } else {
            super.rollback(height);
        }
    }

    @Override
    public void trim(int height) {
        if (multiversion) {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            VersionedEntityDbTable.trim(dataSource, table, height, dbKeyFactory);
        } else {
            super.trim(height);
        }
    }

    @Override
    public final void createSearchIndex(Connection con) throws SQLException {
        if (fullTextSearchColumns != null) {
            log.debug("Creating search index on " + table + " (" + fullTextSearchColumns + ")");
            fullText.createIndex(con, "PUBLIC", table.toUpperCase(), fullTextSearchColumns.toUpperCase());
        }
    }

    private boolean doesNotExceed(int height) {
        if (blockchain == null) blockchain = CDI.current().select(BlockchainImpl.class).get();
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        return blockchain.getHeight() <= height && ! (isPersistent() && blockchainProcessor.isScanning());
    }

}
