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

public abstract class VersionedEntityDbTable<T> extends EntityDbTable<T> {
    private static final Logger LOG = getLogger(VersionedEntityDbTable.class);

    protected VersionedEntityDbTable(String table, KeyFactory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true, null);
    }

    protected VersionedEntityDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns) {
        super(table, dbKeyFactory, multiversion, fullTextSearchColumns);
    }

    protected VersionedEntityDbTable(String table, KeyFactory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, true, fullTextSearchColumns);
    }

    public final boolean delete(T t) {
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
        try (Connection con = db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + dbKeyFactory.getPKColumns()
                     + " FROM " + table + " WHERE height > ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
                     + " WHERE height > ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table
                     + " SET latest = TRUE " + dbKeyFactory.getPKClause() + " AND height ="
                     + " (SELECT MAX(height) FROM " + table + dbKeyFactory.getPKClause() + ")")) {
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
            throw new RuntimeException(e.toString(), e);
        }
        LOG.trace("Rollback for table {} took {} ms", table, System.currentTimeMillis() - startTime);
    }

    static void trim(final TransactionalDataSource dataSource, final String table, final int height, final KeyFactory dbKeyFactory) {
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        long startTime = System.currentTimeMillis();
        try (Connection con = dataSource.getConnection();
             //find acc and max_height of last written record (accounts with one record will be omitted)
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + dbKeyFactory.getPKColumns() + ", MAX(height) AS max_height"
                     + " FROM " + table + " WHERE height < ? GROUP BY " + dbKeyFactory.getPKColumns() + " HAVING COUNT(DISTINCT height) > 1");
//             delete record by ids
             PreparedStatement pstmtDeleteByIds =
                     con.prepareStatement("DELETE FROM " + table + " WHERE db_id IN (SELECT * FROM table(x bigint = ? ))");
             PreparedStatement selectDbIdStatement =
                     con.prepareStatement("SELECT db_id, height FROM " + table + " " + dbKeyFactory.getPKClause());
//             delete record by id
             PreparedStatement pstmtDeletedById =
                     con.prepareStatement("DELETE FROM " + table + " WHERE db_id = ?");
             PreparedStatement pstmtSelectDeleteDeletedIds = con.prepareStatement("SELECT " + dbKeyFactory.getPKColumns() + " FROM " + table + " WHERE height >= ?");
             PreparedStatement pstmtSelectDeleteDeletedCandidates =
                     con.prepareStatement("SELECT DB_ID, " + dbKeyFactory.getPKColumns() + " FROM " + table + " WHERE height < ? AND height >= 0 " +
                             "AND latest = FALSE ");
             PreparedStatement pstmtDeleteDeleted = con.prepareStatement("DELETE FROM " + table + " WHERE height < ? AND height >= 0 AND latest = FALSE "
                     + " AND (" + dbKeyFactory.getPKColumns() + ") NOT IN (SELECT (" + dbKeyFactory.getPKColumns() + ") FROM "
                     + table + " WHERE height >= ?) LIMIT " + propertiesHolder.BATCH_COMMIT_SIZE())) {
            pstmtSelect.setInt(1, height);
            long startDeleteTime;
            long deleted = 0L;
            long deleteStm = 0L;
            long startSelectTime = System.currentTimeMillis();
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                LOG.trace("Select {} time: {}", table, System.currentTimeMillis() - startSelectTime);
                startDeleteTime = System.currentTimeMillis();
                while (rs.next()) {
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
                // changed algo - select all dbkeys from query and insert to hashset, create index for height and latest and select db_key and
                // db_id from table using index. Next filter each account_id from set and delete it.

                long startDeleteDeletedTime = System.currentTimeMillis();
//                int totalDeleteDeleted = deleteDeletedOldAlgo(pstmtDeleteDeleted, height);
                int totalDeleteDeleted = deleteDeletedNewAlgo(pstmtSelectDeleteDeletedIds, pstmtSelectDeleteDeletedCandidates, pstmtDeletedById,
                        dbKeyFactory, height, dataSource);
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

    private static int deleteDeletedNewAlgo(PreparedStatement pstmtSelectDeleteDeletedIds,
                                            PreparedStatement pstmtSelectDeleteDeletedCandidates,
                                            PreparedStatement pstmtDeletedById, KeyFactory dbKeyFactory,
                                            int height,
                                            TransactionalDataSource dataSource) throws SQLException {
        int deleted = 0;
        pstmtSelectDeleteDeletedIds.setInt(1, height);
        Set<DbKey> ids = new HashSet<>();
        try (ResultSet idsRs = pstmtSelectDeleteDeletedIds.executeQuery()) {
            while (idsRs.next()) {
                ids.add(dbKeyFactory.newKey(idsRs));
            }
        }
        pstmtSelectDeleteDeletedCandidates.setInt(1, height);

        try (ResultSet candidatesRs = pstmtSelectDeleteDeletedCandidates.executeQuery()) {
            while (candidatesRs.next()) {
                DbKey dbKey = dbKeyFactory.newKey(candidatesRs);
                if (!ids.contains(dbKey)) {
                    pstmtDeletedById.setLong(1, candidatesRs.getLong(1));
                    pstmtDeletedById.executeUpdate();
                    if (++deleted % 100 == 0) {
                        dataSource.commit(false);
                    }
                }
            }
        }
        dataSource.commit(false);
        return deleted;
    }

//    private  int deleteDeletedOldAlgo(PreparedStatement pstm, int height) throws SQLException {
//        int deleted;
//        int totalDeleted = 0;
//        pstm.setInt(1, height);
//        pstm.setInt(2, height);
//        do {
//            deleted = pstm.executeUpdate();
//            totalDeleted += deleted;
//            TransactionalDataSource dataSource = databaseManager.getDataSource();
//            dataSource.commit(false);
//        } while (deleted >= propertiesHolder.BATCH_COMMIT_SIZE());
//        return totalDeleted;
//    }
}
