/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.db.migrator;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.FullTextTrigger;
import com.apollocurrency.aplwallet.apl.core.db.model.Option;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author al
 */
public class DbMigratorTask {
    private static Logger LOG = LoggerFactory.getLogger(DbMigratorTask.class);

    private PropertiesHolder propertiesHolder;
    private boolean isTestnet;
    private String oldDbPrefix;

    @Inject
    public DbMigratorTask(BlockchainConfig config, PropertiesHolder propertiesHolder) {
        Objects.requireNonNull(propertiesHolder, "Properties holder cannot be null");
        this.propertiesHolder = propertiesHolder;
        this.isTestnet = config.isTestnet();
        this.oldDbPrefix = isTestnet ? "apl.testDb" : "apl.db";
    }

    public void migrateDb() {
        String thirdDbMigrationRequiredOption = Option.get("thirdDbMigrationRequired");
        boolean thirdMigrationRequired = thirdDbMigrationRequiredOption == null || Boolean.parseBoolean(thirdDbMigrationRequiredOption);
        if (thirdMigrationRequired) {
            Option.set("thirdDbMigrationRequired", "true");
            LOG.debug("Db migration required");
            Db.shutdown();
            String targetDbPath =
                    AplCoreRuntime.getInstance().getDbDir().toAbsolutePath().toString() + File.separator + Constants.APPLICATION_DIR_NAME;
            String dbUser = propertiesHolder.getStringProperty(oldDbPrefix + "Username");
            String dbPassword = propertiesHolder.getStringProperty(oldDbPrefix + "Password");
            LegacyDbLocationsProvider legacyDbLocationsProvider = CDI.current().select(LegacyDbLocationsProvider.class).get();
            List<Path> legacyDbDirs = legacyDbLocationsProvider.getDbLocations();
            DbInfoExtractor dbInfoExtractor = new H2DbInfoExtractor(dbUser, dbPassword);
            DbMigrator dbMigrator = new ChainIdDbMigrator(legacyDbDirs, dbInfoExtractor);
            try {
                AppStatus.getInstance().update("Performing database migration");
                Path oldDbPath = dbMigrator.migrate(targetDbPath);
                Db.init();
                FullTextTrigger.init();
                try (Connection connection = Db.getDb().getConnection()) {
                    FullTextTrigger.reindex(connection);
                }
                catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                Option.set("thirdDbMigrationRequired", "false");
                boolean deleteOldDb = propertiesHolder.getBooleanProperty("apl.deleteOldDbAfterMigration");
                if (deleteOldDb && oldDbPath != null) {
                    Option.set("oldDbPath", oldDbPath.toAbsolutePath().toString());
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
        performDbMigrationCleanup();
    }

    public void deleteAllWithExclusion(Path pathToDelete, Path pathToExclude) throws IOException {

        List<Path> excludedPaths = Files.walk(pathToExclude.normalize()).collect(Collectors.toList());
        if (pathToExclude.startsWith(pathToDelete)) {
            Path relativePath = pathToDelete.relativize(pathToExclude);
            for (Path aRelativePath : relativePath) {
                excludedPaths.add(aRelativePath);
            }
            excludedPaths.add(pathToDelete.normalize());
        }
        Files.walkFileTree(pathToDelete.normalize(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!excludedPaths.contains(file)) {
                    Files.delete(file);
                }
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!excludedPaths.contains(dir)) {
                    Files.delete(dir);
                }
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    public void performDbMigrationCleanup() {
        String targetDbDir = AplCoreRuntime.getInstance().getDbDir().toAbsolutePath().toString();
        String oldDbPathOption = Option.get("oldDbPath");
        if (oldDbPathOption != null) {
            Path oldDbPath = Paths.get(oldDbPathOption);
            if (Files.exists(oldDbPath)) {
                try {
                    deleteAllWithExclusion(oldDbPath, Paths.get(targetDbDir));
                    Option.delete("oldDbPath");
                }
                catch (IOException e) {
                    LOG.error("Unable to delete old db");
                }
            } else {
                Option.delete("oldDbPath");
            }
        }
    }
}
