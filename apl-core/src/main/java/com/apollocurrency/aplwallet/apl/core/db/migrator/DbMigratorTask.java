/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.db.migrator;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.db.FullTextTrigger;
import com.apollocurrency.aplwallet.apl.core.db.model.Option;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author al
 */
public class DbMigratorTask {
    private static Logger LOG = LoggerFactory.getLogger(DbMigratorTask.class);

    private PropertiesHolder propertiesHolder;
    private boolean isTestnet;
    private String oldDbPrefix;

    @Inject
    public DbMigratorTask(boolean isTestnet, PropertiesHolder propertiesHolder) {
        Objects.requireNonNull(propertiesHolder, "Properties holder cannot be null");
        this.propertiesHolder = propertiesHolder;
        this.isTestnet = isTestnet;
        this.oldDbPrefix = isTestnet ? "apl.testDb" : "apl.db";
    }
        public void migrateDb() {
            String secondDbMigrationRequired = Option.get("secondDbMigrationRequired");
            boolean secondMigrationRequired = secondDbMigrationRequired == null || Boolean.parseBoolean(secondDbMigrationRequired);
            if (secondMigrationRequired) {
                Option.set("secondDbMigrationRequired", "true");
                LOG.debug("Db migration required");
                Db.shutdown();
                String targetDbDir = AplCoreRuntime.getInstance().getDbDir().toAbsolutePath().toString();
                String dbName = propertiesHolder.getStringProperty(oldDbPrefix + "Name");
                String dbUser = propertiesHolder.getStringProperty(oldDbPrefix + "Username");
                String dbPassword = propertiesHolder.getStringProperty(oldDbPrefix + "Password");
                List<String> legacyDbDirs = getLegacyDirs();
                //                String legacyDbDir = AplCoreRuntime.getInstance().getDbDir(dbDir, null, false);
//                String chainIdDbDir = AplCoreRuntime.getInstance().getDbDir(dbDir, true);
                DbInfoExtractor dbInfoExtractor = new H2DbInfoExtractor(dbName, dbUser, dbPassword);
//                com.apollocurrency.aplwallet.apl.core.chainid.DbMigrator dbMigrator = new ChainIdDbMigrator(chainIdDbDir, legacyDbDir, dbInfoExtractor);
//                try {
                    AppStatus.getInstance().update("Performing database migration");
//                    Path oldDbPath = dbMigrator.migrate(targetDbDir);
                    Db.init();
                    try (Connection connection = Db.getDb().getConnection()) {
                        FullTextTrigger.reindex(connection);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
//                    AplGlobalObjects.createBlockDb(new ConnectionProviderImpl());
                    Option.set("secondDbMigrationRequired", "false");
                    boolean deleteOldDb = propertiesHolder.getBooleanProperty("apl.deleteOldDbAfterMigration");
//                    if (deleteOldDb && oldDbPath != null) {
//                        Option.set("oldDbPath", oldDbPath.toAbsolutePath().toString());
//                    }
//                }
//                catch (IOException e) {
//                    throw new RuntimeException(e.toString(), e);
//                }
            }
            performDbMigrationCleanup();
        }

    private List<String> getLegacyDirs() {
        return Arrays.asList();
    }

    public void performDbMigrationCleanup() {
            String dbDir = propertiesHolder.getStringProperty(oldDbPrefix + "Dir");
            String targetDbDir = AplCoreRuntime.getInstance().getDbDir().toAbsolutePath().toString();
            String oldDbPathOption = Option.get("oldDbPath");
            if (oldDbPathOption != null) {
                Path oldDbPath = Paths.get(oldDbPathOption);
                if (Files.exists(oldDbPath)) {
                    try {
                        ChainIdDbMigrator.deleteAllWithExclusion(oldDbPath, Paths.get(targetDbDir));
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
