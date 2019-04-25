/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;


import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.CDI;

public abstract class VersionedDeletableEntityDbTable<T> extends EntityDbTable<T> {
    private static final Logger LOG = getLogger(VersionedDeletableEntityDbTable.class);

    protected VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true, null);
    }

    protected VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, true, fullTextSearchColumns);
    }

    @Override
    public boolean delete(T t) {
        return delete(t, false);
    }

    public final boolean delete(T t, boolean keepInCache) {
        if (t == null) {
            return false;
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
        DbKey dbKey = dbKeyFactory.newKey(t);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtCount = con.prepareStatement("SELECT 1 FROM " + table
                     + dbKeyFactory.getPKClause() + " AND height < ? LIMIT 1")) {
            int i = dbKey.setPK(pstmtCount);
            pstmtCount.setInt(i, blockchain.getHeight());
            try (ResultSet rs = pstmtCount.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
                        dbKey.setPK(pstmt);
                        pstmt.executeUpdate();
                        save(con, t);
                        pstmt.executeUpdate(); // delete after the save
                    }
                    return true;
                } else {
                    try (PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause())) {
                        dbKey.setPK(pstmtDelete);
                        return pstmtDelete.executeUpdate() > 0;
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        finally {
            if (!keepInCache) {
                dataSource.getCache(table).remove(dbKey);
            }
        }
    }

    static void rollback(final TransactionalDataSource db, final String table, final int height, final KeyFactory dbKeyFactory) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        long startTime = System.currentTimeMillis();
        String sql = "UPDATE " + table
                + " SET latest = TRUE " + dbKeyFactory.getPKClause() + " AND height ="
                + " (SELECT MAX(height) FROM " + table + dbKeyFactory.getPKClause() + ")";
        LOG.trace(sql);
        try (Connection con = db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + dbKeyFactory.getPKColumns()
                     + " FROM " + table + " WHERE height > ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
                     + " WHERE height > ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement(sql)) {
            pstmtSelectToDelete.setInt(1, height);
            List<DbKey> dbKeys = new ArrayList<>();
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    dbKeys.add(dbKeyFactory.newKey(rs));
                }
            }

            if (dbKeys.size() > 0) {
                LOG.trace("Rollback table {} found {} records to update to latest", table, dbKeys.size());
            }

            pstmtDelete.setInt(1, height);
            int deletedRecordsCount = pstmtDelete.executeUpdate();

            if (deletedRecordsCount > 0) {
                LOG.trace("Rollback table {} deleting {} records", table, deletedRecordsCount);
            }

            for (DbKey dbKey : dbKeys) {
                int i = 1;
                i = dbKey.setPK(pstmtSetLatest, i);
                i = dbKey.setPK(pstmtSetLatest, i);
                pstmtSetLatest.executeUpdate();
                //DatabaseManager.getCache(table).remove(dbKey);
            }
        }
        catch (SQLException e) {
            LOG.error("Error", e);
            throw new RuntimeException(e.toString(), e);
        }
        LOG.trace("Rollback for table {} took {} ms", table, System.currentTimeMillis() - startTime);
    }

    /**
     * <p>Delete old data from db before target height. Leave last actual entry for each entity to allow rollback to target height</p>
     * <p>Also will delete blockchain 'deleted' entries with latest=false which not exist at height greater than target height</p>
     * <p>WARNING! Do not trim to your current blockchain height! It will delete all history data and you will not be able to rollback and switch to another fork</p>
     * @param dataSource   db datasource, where trim should be performed
     * @param table        name of the db table to trim
     * @param height       target height of blockchain for trimming, should be less or equal to minRollbackHeight to allow rollback to such height
     * @param dbKeyFactory key factory to retrieve entities
     * <p>Example:</p>
     *                     <pre>{@code
     *                     db_id     account   balance    height    latest
     *                     0         00         10        0         true
     *                     1         000        10        100       false
     *                     2         200        25        100       false
     *                     3         200        50        125       false
     *                     4         100        5         175       false
     *                     5         200        125       200       false
     *                     6         100        5         200       false
     *                     7         200        6         200       true
     *                     8         200        6         220       true
     *                     9         100        80        230       true
     *                     10        500        100       240       true
     *                     }</pre>
     * <p>
     *                     Trim to height 201 will result in
     *                     <pre>{@code
     *                     db_id     account   balance    height    latest
     *                     0         00         10        0         true
     *                     4         100        5         175       false
     *                     5         200        125       200       false
     *                     7         200        6         210       false
     *                     8         200        6         220       true
     *                     9         100        80        230       true
     *                     10        500        100       240       true
     *                     }</pre>
     * </p>
     */
    static void trim(final TransactionalDataSource dataSource, final String table, final int height, final KeyFactory dbKeyFactory) {
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        long startTime = System.currentTimeMillis();
        try (Connection con = dataSource.getConnection();
             //find acc and max_height of last written record (accounts with one record will be omitted)
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + dbKeyFactory.getPKColumns() + ", MAX(height) AS max_height"
                     + " FROM " + table + " WHERE height < ? GROUP BY " + dbKeyFactory.getPKColumns() + " HAVING COUNT(DISTINCT height) > 1")) {
            pstmtSelect.setInt(1, height);
            long startDeleteTime;
            long deleted = 0L;
            long deleteStm = 0L;
            long startSelectTime = System.currentTimeMillis();
            try (ResultSet rs = pstmtSelect.executeQuery();
                 PreparedStatement pstmtDeleteByIds =
                         con.prepareStatement("DELETE FROM " + table + " WHERE db_id IN (SELECT * FROM table(x bigint = ? ))");
                 PreparedStatement selectDbIdStatement =
                         con.prepareStatement("SELECT db_id, height FROM " + table + " " + dbKeyFactory.getPKClause())) {
                LOG.trace("Select {} time: {}", table, System.currentTimeMillis() - startSelectTime);
                startDeleteTime = System.currentTimeMillis();
                while (rs.next()) {
                    List<Long> keys = selectDbIds(selectDbIdStatement, rs, dbKeyFactory);
                    if (!keys.isEmpty()) {
                        pstmtDeleteByIds.setObject(1, keys.toArray());
                        deleted += pstmtDeleteByIds.executeUpdate();
                        deleteStm++;
                        if (deleted % 100 == 0) {
                            dataSource.commit(false);
                        }
                    }
                }
                dataSource.commit(false);
                LOG.trace("Delete time {} for table {}: stm - {}, deleted - {}", System.currentTimeMillis() - startDeleteTime, table,
                        deleteStm, deleted);
                long startDeleteDeletedTime = System.currentTimeMillis();
                int totalDeleteDeleted = deleteDeletedNewAlgo(con, table, dbKeyFactory, height, dataSource);
                LOG.trace("Delete deleted time for table {} is: {}, deleted - {}", table, System.currentTimeMillis() - startDeleteDeletedTime, totalDeleteDeleted);
            }
            long trimTime = System.currentTimeMillis() - startTime;
            if (trimTime > propertiesHolder.TRIM_TRANSACTION_TIME_THRESHHOLD()) {
                LOG.debug("Trim for table {} took {} ms", table, trimTime);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static List<Long> selectDbIds(PreparedStatement selectDbIdStatement, ResultSet rs, KeyFactory dbKeyFactory) throws SQLException {
        DbKey dbKey = dbKeyFactory.newKey(rs);
        dbKey.setPK(selectDbIdStatement);
        List<Long> keys = new ArrayList<>();
        int maxHeight = rs.getInt("max_height");
        try (ResultSet dbIdsSet = selectDbIdStatement.executeQuery()) {
            while (dbIdsSet.next()) {
                int currentHeight = dbIdsSet.getInt(2);
                if (currentHeight < maxHeight && currentHeight >= 0) {
                    keys.add(dbIdsSet.getLong(1));
                }
            }
        }
        return keys;
    }

    // changed algo - select all dbkeys from query and insert to hashset, create index for height and latest and select db_key and
    // db_id from table using index. Next filter each account_id from set and delete it.
    private static int deleteDeletedNewAlgo(Connection con, String table, KeyFactory dbKeyFactory,
                                            int height,
                                            TransactionalDataSource dataSource) throws SQLException {
        int deleted = 0;
        Set<DbKey> dbKeys = selectExistingDbKeys(con, table, height, dbKeyFactory);
        try (PreparedStatement pstmtSelectDeleteCandidates =
                     con.prepareStatement("SELECT DB_ID, " + dbKeyFactory.getPKColumns() + " FROM " + table + " WHERE height < ? AND height >= 0 " +
                             "AND latest = FALSE ")) {
            pstmtSelectDeleteCandidates.setInt(1, height);

            try (ResultSet candidatesRs = pstmtSelectDeleteCandidates.executeQuery();
                 PreparedStatement pstmtDeleteByDbId =
                         con.prepareStatement("DELETE FROM " + table + " WHERE db_id = ?")) {
                while (candidatesRs.next()) {
                    DbKey dbKey = dbKeyFactory.newKey(candidatesRs);
                    if (!dbKeys.contains(dbKey)) {
                        long dbId = candidatesRs.getLong(1);
                        deleteByDbId(pstmtDeleteByDbId, dbId);
                        if (++deleted % 100 == 0) {
                            dataSource.commit(false);
                        }
                    }
                }
            }
        }
        dataSource.commit(false);
        return deleted;
    }

    private static int deleteByDbId(PreparedStatement pstmtDeleteByDbId, long dbId) throws SQLException {
        pstmtDeleteByDbId.setLong(1, dbId);
        return pstmtDeleteByDbId.executeUpdate();

    }

    private static Set<DbKey> selectExistingDbKeys(Connection con, String table, int height, KeyFactory keyFactory) throws SQLException {
        Set<DbKey> dbKeys = new HashSet<>();
        try (PreparedStatement pstmtSelectExistingIds = con.prepareStatement("SELECT " + keyFactory.getPKColumns() + " FROM " + table + " WHERE height >= ?")) {
            pstmtSelectExistingIds.setInt(1, height);
            try (ResultSet idsRs = pstmtSelectExistingIds.executeQuery()) {
                while (idsRs.next()) {
                    dbKeys.add(keyFactory.newKey(idsRs));
                }
            }
        }
        return dbKeys;
    }
}
