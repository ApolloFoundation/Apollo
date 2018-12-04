/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import org.slf4j.Logger;

import java.sql.*;

import static org.slf4j.LoggerFactory.getLogger;

public class PublicKeyMigration {
    private static final Logger LOG = getLogger(PublicKeyMigration.class);

    public static void init() {
        Connection con;
        boolean isInTransaction = false;
        try {
            if (Db.getDb().isInTransaction()) {
                isInTransaction = true;
                con = Db.getDb().getConnection();
            } else {
                con = Db.getDb().beginTransaction();
            }

            try (Statement stmt = con.createStatement();
                 PreparedStatement selectGenesisKeysStatement = con.prepareStatement((
                         "SELECT * FROM public_key where DB_ID between ? AND ?"));
            ) {
                int totalNumberOfGenesisKeys = 0;
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) from public_key where height = 0")) {
                    rs.next();
                    totalNumberOfGenesisKeys = rs.getInt(1);
                }
                String message = "Performing public keys migration";
                LOG.info(message);
                boolean isMigrationInterrupted = false;
                try {
                    stmt.executeUpdate("DROP INDEX genesis_public_key_account_id_height_idx");
                    isMigrationInterrupted = true;
                    LOG.debug("Migration was interrupted");
                }
                catch (SQLException e) {
                    //ignore
                }
                Apl.getRuntimeMode().updateAppStatus(message);
                //create copy of public_key table
                if (!isMigrationInterrupted) {

                    stmt.executeUpdate("DROP TABLE IF EXISTS genesis_public_key");
                    stmt.executeUpdate("CREATE TABLE genesis_public_key (db_id IDENTITY, account_id BIGINT NOT NULL, public_key BINARY(32), height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, latest BOOLEAN NOT NULL DEFAULT TRUE)");
                    //copy genesis keys if exists
                    long minDbId;
                    long maxDbId;
                    try (ResultSet rs = stmt.executeQuery("SELECT MIN(db_id) as min_db_id, MAX(db_id) as max_db_id from PUBLIC_KEY where HEIGHT = 0")) {
                        rs.next();
                        minDbId = rs.getLong("min_db_id");
                        maxDbId = rs.getLong("max_db_id");
                    }
                    LOG.info("Copy genesis public keys");
                    selectGenesisKeysStatement.setLong(1, minDbId);
                    selectGenesisKeysStatement.setLong(2, maxDbId);
                    try (ResultSet rs = selectGenesisKeysStatement.executeQuery(); PreparedStatement pstm = con.prepareStatement("INSERT INTO genesis_public_key " +
                            "VALUES (?, ?, ?, ?, ?)")) {
                        int counter = 0;
                        while (rs.next()) {
                            pstm.setLong(1, rs.getLong(1));
                            pstm.setLong(2, rs.getLong(2));
                            pstm.setBytes(3, rs.getBytes(3));
                            pstm.setInt(4, rs.getInt(4));
                            pstm.setBoolean(5, rs.getBoolean(5));
                            pstm.addBatch();
                            if (++counter % 500 == 0) {
                                pstm.executeBatch();
                                Db.getDb().commitTransaction();
                                LOG.debug("Copied {} / {}", counter, totalNumberOfGenesisKeys);
                            }
                        }
                        pstm.executeBatch();
                    }
                }
                //delete genesis keys
                int deleted;
                int totalDeleted = 0;
                stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS genesis_public_key_account_id_height_idx on genesis_public_key(account_id, height)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS genesis_public_key_height_idx on genesis_public_key(height)");
                do {
                    deleted = stmt.executeUpdate("DELETE FROM public_key where height = 0 LIMIT " + Constants.BATCH_COMMIT_SIZE);
                    totalDeleted += deleted;
                    LOG.debug("Migration performed for {}/{} public keys", totalDeleted, totalNumberOfGenesisKeys);
                    Db.getDb().commitTransaction();
                } while (deleted == Constants.BATCH_COMMIT_SIZE);
                //add indices
            }
            Db.getDb().commitTransaction();
        }
        catch (SQLException e) {
            Db.getDb().rollbackTransaction();
            throw new RuntimeException(e.toString(), e);
        }
        finally {
            if (!isInTransaction) {
                Db.getDb().endTransaction();
            }
        }
    }
}
