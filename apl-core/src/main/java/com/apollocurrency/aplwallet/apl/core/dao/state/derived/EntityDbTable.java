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

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import org.slf4j.Logger;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class EntityDbTable<T extends DerivedEntity> extends BasicDbTable<T> implements EntityDbTableInterface<T> {
    private static final Logger log = getLogger(EntityDbTable.class);
    private final String defaultSort;

    public EntityDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns,
                         DatabaseManager databaseManager,
                         Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(table, dbKeyFactory, multiversion, databaseManager,
                fullTextOperationDataEvent, fullTextSearchColumns);
        this.defaultSort = " ORDER BY " + (multiversion ? dbKeyFactory.getPKColumns() : " height DESC, db_id DESC ");
    }

    /***
     * Persist new entitity into db
     * Warning!
     * This method must support MERGE for already existing entities with same height and dbKey (when it is possible to have such entities by business logic)
     * @param con db connection, usually opened under block accepting procedure
     * @param entity entity object ro save
     * @throws SQLException if any db error occurred.
     */
    public abstract void save(Connection con, T entity) throws SQLException;

    @Override
    public String defaultSort() {
        return defaultSort;
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

    /**
     * Gets an entity.
     * <p>
     * Note that validation happens at service level.
     *
     * @param dbKey
     * @param height
     * @return
     */
    @Override
    public T get(DbKey dbKey, int height) {
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
            if (rs.next() && dbKey != null) {
                log.debug("Multiple records found. Table: {} Key: {}", table, dbKey.toString());
                throw new RuntimeException("Multiple records found. Table: " + table + " Key: " + dbKey.toString());
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

    /**
     * Gets an iterator on entities.
     * <p>
     * Note that validation happens at service level.
     *
     * @param dbClause
     * @param height
     * @param from
     * @param to
     * @param sort
     * @return
     */
    @Override
    public final DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort) {
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
            DbKey dbKey = null;
            if (doCache) {
                dbKey = keyFactory.newKey(rs);
            }
            T t = load(connection, rs, dbKey);
            return t;
        });
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

    /**
     * Gets entity count.
     * <p>
     * Note that validation happens at service level.
     *
     * @param dbClause
     * @param height
     * @return
     */
    @Override
    public final int getCount(DbClause dbClause, int height) {
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
        } finally {
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
            // update only entity with existing db_id, assuming that 't'
            // entity is the latest and exists on the top of the blockchain
            if (multiversion && !t.isNew()) {
                try (
                    PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                        + " SET latest = FALSE WHERE db_id = ?")
                ) {
                    pstmt.setLong(1, t.getDbId());
                    pstmt.executeUpdate();
                }
            }
            restoreDeletedColumnIfSupported(con, dbKey, t);
            save(con, t);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * If 'delete' operation supported for the table and 'deleted=true' entity for {@code dbKey} exists on the
     * current blockchain height, then will restore deleted=false column value for entity specified by {@code dbKey},
     * which exists on height less than current blockchain height.
     * It is a compensation action applied to 'delete' operation on same height
     * Example
     * <pre>
     *     GIVEN
     *     account table content:
     *     db_id   id    balance  height  latest  deleted
     *     10      1     25        1       false   true
     *     20      1     0         2       false   true
     * {@link Blockchain#getHeight()} return 2 (block table MAX(height) = 2)
     *     WHEN
     *     Account restoredAccount = Account.builder()
     *                              .id(1)
     *                              .balance(10)
     *                              .height(2)
     *                              .latest(true)
     *                              .deleted(false)
     *                              .build();
     *     accountTable.insert(restoredAccount);
     *     THEN
     *     db_id   id    balance  height  latest  deleted
     *     10      1     25        1       false  false  --- restored 'deleted' column value to 'false' (deletion compensation)
     *     20      1     10        2       true   false  --- record was merged on same height
     * </pre>
     *
     * @param con   db connection under transaction running
     * @param dbKey unique key for entity identifying
     * @throws SQLException if any db error occurred
     */
    private void restoreDeletedColumnIfSupported(Connection con, DbKey dbKey, T t) throws SQLException {
        if (supportDelete()) {
            // replaced setting 'height' by entity field 'height' instead of receiving from CDI Blockchain
            int height = t.getHeight();
            try (PreparedStatement thisExistsAndDeleted = con.prepareStatement("SELECT 1 from " + table + keyFactory.getPKClause() + " AND height = ? AND deleted = true")) { // checking our entity existence on current blockchain height in 'deleted=true' state
                int index = dbKey.setPK(thisExistsAndDeleted, 1);
                thisExistsAndDeleted.setInt(index, height);
                try (ResultSet rs = thisExistsAndDeleted.executeQuery()) {
                    if (rs.next()) { // our entity exists and was deleted - compensation required (point of no return)
                        try (PreparedStatement selectPrevDeleted = con.prepareStatement("SELECT db_id FROM " + table + keyFactory.getPKClause() +
                            " AND height < ? AND deleted=true ORDER BY db_id DESC LIMIT 1"); // find db_id of the most recent previous record (deleted=true)
                             PreparedStatement updatePrevDeleted = con.prepareStatement("UPDATE " + table + " SET deleted = false WHERE db_id = ?")) { // perform compensation
                            int selectPrevIndex = dbKey.setPK(selectPrevDeleted, 1);
                            selectPrevDeleted.setInt(selectPrevIndex, height);
                            try (ResultSet prevDeletedRs = selectPrevDeleted.executeQuery()) {
                                if (prevDeletedRs.next()) {
                                    long prevDbId = prevDeletedRs.getLong(1);
                                    updatePrevDeleted.setLong(1, prevDbId);
                                    updatePrevDeleted.executeUpdate();
                                } else {
                                    throw new IllegalStateException("Unable to find previous record for dbKey " + dbKey + ", inconsistent database state (maybe 'delete' flow is broken)");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
