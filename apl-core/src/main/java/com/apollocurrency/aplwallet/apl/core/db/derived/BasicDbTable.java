/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provide rollback and trim multiversion implementations and hold common parameters such as multiversion and keyfactory
 */
public abstract class BasicDbTable<T> extends DerivedDbTable<T> {
    private static final Logger LOG = getLogger(BasicDbTable.class);

    protected KeyFactory<T> keyFactory;
    protected boolean multiversion;

    protected BasicDbTable(String table, KeyFactory<T> keyFactory, boolean multiversion, boolean init) {
        super(table, init);
        this.keyFactory = keyFactory;
        this.multiversion = multiversion;
    }

    public KeyFactory<T> getDbKeyFactory() {
        return keyFactory;
    }

    public boolean isMultiversion() {
        return multiversion;
    }

    @Override
    public int rollback(int height) {
        if (multiversion) {
            return doMultiversionRollback(height);
        } else {
            return super.rollback(height);
        }
    }

    private int doMultiversionRollback(int height) {
        int deletedRecordsCount;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        long startTime = System.currentTimeMillis();
        String sql = "UPDATE " + table
                + " SET latest = TRUE " + (getDeletedSetStatementIfSupported(false)) + keyFactory.getPKClause() + " AND height ="
                + " (SELECT MAX(height) FROM " + table + keyFactory.getPKClause() + ")";
        LOG.trace(sql);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + keyFactory.getPKColumns()
                     + " FROM " + table + " WHERE height > ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
                     + " WHERE height > ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement(sql)) {
            pstmtSelectToDelete.setInt(1, height);
            List<DbKey> dbKeys = new ArrayList<>();
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    dbKeys.add(keyFactory.newKey(rs));
                }
            }

            if (dbKeys.size() > 0) {
                LOG.trace("Rollback table {} found {} records to update to latest", table, dbKeys.size());
            }

            pstmtDelete.setInt(1, height);
            deletedRecordsCount = pstmtDelete.executeUpdate();

            if (deletedRecordsCount > 0) {
                LOG.trace("Rollback table {} deleting {} records", table, deletedRecordsCount);
            }

            for (DbKey dbKey : dbKeys) {
                int i = 1;
                i = dbKey.setPK(pstmtSetLatest, i);
                i = dbKey.setPK(pstmtSetLatest, i);
                pstmtSetLatest.executeUpdate();
            }
        }
        catch (SQLException e) {
            LOG.error("Error", e);
            throw new RuntimeException(e.toString(), e);
        }
        LOG.trace("Rollback for table {} took {} ms", table, System.currentTimeMillis() - startTime);
        return deletedRecordsCount;
    }


    @Override
    public void trim(int height) {
        if (multiversion) {
            doMultiversionTrim(height);
        } else {
            super.trim(height);
        }
    }

    /**
     * <p>Delete old data from db before target height. Leave last actual entry for each entity to allow rollback to target height</p>
     * <p>Also will delete blockchain 'deleted' entries with latest=false which not exist at height greater than target height</p>
     * <p>WARNING! Do not trim to your current blockchain height! It will delete all history data and you will not be able to rollback and switch to another fork</p>
     * @param height       target height of blockchain for trimming, should be less or equal to minRollbackHeight to allow rollback to such height
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
    private void doMultiversionTrim(final int height) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        long startTime = System.currentTimeMillis();
        try (Connection con = dataSource.getConnection();
             //find acc and max_height of last written record (accounts with one record will be omitted)
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + keyFactory.getPKColumns() + ", MAX(height) AS max_height"
                     + " FROM " + table + " WHERE height < ? GROUP BY " + keyFactory.getPKColumns() + " HAVING COUNT(DISTINCT height) > 1")) {
            pstmtSelect.setInt(1, height);
            long startDeleteTime, deleted = 0L, deleteStm = 0L, startSelectTime = System.currentTimeMillis();
            try (ResultSet rs = pstmtSelect.executeQuery();
                 PreparedStatement pstmtDeleteById =
                         con.prepareStatement("DELETE FROM " + table + " WHERE db_id = ?");
                 PreparedStatement selectDbIdStatement =
                         con.prepareStatement("SELECT db_id, height " + getDeletedColumnIfSupported() + " FROM " + table + " " + keyFactory.getPKClause())) {
                LOG.trace("Select {} time: {}", table, System.currentTimeMillis() - startSelectTime);
                startDeleteTime = System.currentTimeMillis();

                while (rs.next()) {
                    EntityDeleteSpec deleteSpec = selectDbIds(selectDbIdStatement, rs);
                    // TODO migrate to PreparedStatement.addBatch for another db
                    for (Long id :deleteSpec.keys) {
                        deleteByDbId(pstmtDeleteById, id);
                        deleted++;
                        deleteStm++;
                        if (deleted % 100 == 0) {
                            dataSource.commit(false);
                        }
                    }
                    if (deleteSpec.lastDbId != 0) {
                        deleteByDbId(pstmtDeleteById, deleteSpec.lastDbId);
                    }
                }
                dataSource.commit(false);
                LOG.trace("Delete time {} for table {}: stm - {}, deleted - {}", System.currentTimeMillis() - startDeleteTime, table,
                        deleteStm, deleted);
            }
            long trimTime = System.currentTimeMillis() - startTime;
            if (trimTime > 1000) {
                LOG.debug("Trim for table {} took {} ms", table, trimTime);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private String getDeletedColumnIfSupported() {
        return supportDelete() ? ", deleted" : "";
    }
    private String getDeletedSetStatementIfSupported(boolean deleted) {
        return supportDelete() ? ", deleted = " + deleted + " ": "";
    }

    private  EntityDeleteSpec selectDbIds(PreparedStatement selectDbIdStatement, ResultSet rs) throws SQLException {
        DbKey dbKey = keyFactory.newKey(rs);
        dbKey.setPK(selectDbIdStatement);
        Set<Long> keys = new HashSet<>();
        int maxHeight = rs.getInt("max_height");
        boolean lastDeleted = false;
        long lastDbId = 0;
        int maxDeletedHeight = -1, overallMaxHeight = -1;
        try (ResultSet dbIdsSet = selectDbIdStatement.executeQuery()) {
            while (dbIdsSet.next()) {
                int currentHeight = dbIdsSet.getInt(2);
                boolean entryDeleted = supportDelete() && dbIdsSet.getBoolean(3);
                long dbId = dbIdsSet.getLong(1);
                if (currentHeight == maxHeight) {
                    lastDeleted = entryDeleted;
                    lastDbId = dbId;
                } else if (currentHeight < maxHeight && currentHeight >= 0) {
                    if (overallMaxHeight < currentHeight) {
                        overallMaxHeight = currentHeight;
                    }
                    if (entryDeleted && maxDeletedHeight < currentHeight) {
                        maxDeletedHeight = currentHeight;
                    }
                    keys.add(dbId);
                }
            }
        }
        if (maxDeletedHeight != overallMaxHeight || !lastDeleted) {
            lastDbId = 0;
        }
        return new EntityDeleteSpec(lastDbId, keys);
    }

    @Data
    @AllArgsConstructor
    private static class EntityDeleteSpec {
        private long lastDbId;
        private Set<Long> keys;
    }

    private void deleteByDbId(PreparedStatement pstmtDeleteByDbId, long dbId) throws SQLException {
        pstmtDeleteByDbId.setLong(1, dbId);
        pstmtDeleteByDbId.executeUpdate();

    }
}
