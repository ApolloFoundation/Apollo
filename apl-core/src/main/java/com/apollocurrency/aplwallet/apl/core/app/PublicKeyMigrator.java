/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.sql.*;
import javax.enterprise.inject.spi.CDI;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PublicKeyMigrator {
    private static final Logger LOG = getLogger(PublicKeyMigrator.class);
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();

    public void migrate() {
        Connection con;
        boolean isInTransaction = false;
        // start transaction or use already started
        try {
            if (Db.getDb().isInTransaction()) {
                isInTransaction = true;
                con = Db.getDb().getConnection();
            } else {
                con = Db.getDb().beginTransaction();
            }
            try (Statement stmt = con.createStatement()) {
//                check genesis keys
                int totalNumberOfGenesisKeys;
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) from public_key where height = 0")) {
                    rs.next();
                    totalNumberOfGenesisKeys = rs.getInt(1);
                }
                if (totalNumberOfGenesisKeys == 0) {
                    LOG.debug("No genesis keys for migration");
                    return;
                }
                LOG.info("Performing public keys migration");
                // find db_id range of genesis public keys
                long minDbId;
                long maxDbId;
                try (ResultSet rs = stmt.executeQuery("SELECT MIN(db_id) as min_db_id, MAX(db_id) as max_db_id from PUBLIC_KEY where HEIGHT = 0")) {
                    rs.next();
                    minDbId = rs.getLong("min_db_id");
                    maxDbId = rs.getLong("max_db_id");
                }
                LOG.info("Copy genesis public keys");
                // copy genesis public keys into the new table
                try (PreparedStatement pstmt = con.prepareStatement(
                        "INSERT INTO genesis_public_key (db_id, account_id, public_key, height, latest )" +
                                " select * FROM public_key where DB_ID between ? AND ?")) {
                    pstmt.setLong(1, minDbId);
                    pstmt.setLong(2, maxDbId);
                    pstmt.executeUpdate();
                }
                //delete genesis keys
                int deleted;
                int totalDeleted = 0;
                do {
                    deleted = stmt.executeUpdate("DELETE FROM public_key where height = 0 LIMIT " + propertiesHolder.BATCH_COMMIT_SIZE());
                    totalDeleted += deleted;
                    LOG.debug("Migration performed for {}/{} public keys", totalDeleted, totalNumberOfGenesisKeys);
                } while (deleted == propertiesHolder.BATCH_COMMIT_SIZE());
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
