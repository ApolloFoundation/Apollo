/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.slf4j.LoggerFactory.getLogger;

public class ChainIdDbMigration {
    private static final Logger LOG = getLogger(ChainIdDbMigration.class);

    static void migrate() {

        boolean dbMigrationRequired = false;
        try (Connection con = Db.getDb().getConnection(); Statement stmt = con.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM option WHERE name = 'isChainIdMigrated' AND LCASE(value) = 'true'")) {
                if (!rs.next()) {
                    dbMigrationRequired = true;
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        if (dbMigrationRequired) {
            LOG.info("Required db migration for applying chainId");
            Db.shutdown();
            String oldDbDir = Apl.getOldDbDir(Apl.getStringProperty(Db.PREFIX + "Dir"));
            Db.init(oldDbDir);
            Path oldDbDirPath = Paths.get(oldDbDir);
            int numberOfBlocks = 0;
            try (Connection con = Db.getDb().getConnection();
                 Statement stmt = con.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM block")) {
                    rs.next();
                    numberOfBlocks = rs.getInt(1);
                }

            }
            catch (SQLException e) {
                LOG.error("Cannot read data from old db", e);
                throw new RuntimeException(e.toString(), e);
            }
            Db.shutdown();
            if (numberOfBlocks > 0) {
                Path chainIdDbDirPath = Paths.get(Apl.getDbDir(Apl.getStringProperty(Db.PREFIX + "Dir")));
                LOG.info("Copying db from {} to {}", oldDbDirPath, chainIdDbDirPath);

                try {
                    Files.walk(oldDbDirPath)
                            .forEach(source -> {
                                try {
                                    Files.copy(source, chainIdDbDirPath.resolve(oldDbDirPath.relativize(source)));
                                }
                                catch (IOException e) {
                                    throw new RuntimeException(e.toString(), e);
                                }
                            });
                }
                catch (IOException e) {
                    throw new RuntimeException(e.toString(), e);
                }

            }
            Db.init();
            try (Connection con = Db.getDb().getConnection();
                 Statement stmt = con.createStatement()) {
                if (numberOfBlocks > 0) {

                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM block")) {
                        rs.next();
                        int actualNumberOfBlocks = rs.getInt(1);
                        if (numberOfBlocks != actualNumberOfBlocks) {
                            LOG.error("Db was copied with errors. Restart your wallet.");
                            System.exit(-1);
                        }
                    }
                    try {
                        Db.removeDb(oldDbDirPath);
                    }
                    catch (IOException e) {
                        LOG.info("Delete old db manually from " + oldDbDir, e);
                    }
                }
                stmt.executeUpdate("INSERT INTO option values ('isChainIdMigrated', 'true')");

            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }
}
