/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;

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
            String dbUrl = String.format("jdbc:%s:%s", Apl.getStringProperty(Db.PREFIX + "Type"), oldDbDir);
            Db.init(dbUrl);
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
                    Files.walk(oldDbDirPath.getParent())
                            .forEach(source -> {
                                try {
                                    if (Files.isDirectory(source)) {
                                        Path targetPath = chainIdDbDirPath.resolve(oldDbDirPath.relativize(source));
                                        if (Files.notExists(targetPath)) {
                                            Files.createDirectory(targetPath);
                                        }
                                    } else {
                                        Files.copy(source, chainIdDbDirPath.resolve(oldDbDirPath.relativize(source)), StandardCopyOption.REPLACE_EXISTING);
                                    }
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
                        Db.removeDb(oldDbDirPath.getParent());
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
