/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.db.migrator;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.AplGlobalObjects;
import com.apollocurrency.aplwallet.apl.core.app.ConnectionProviderImpl;
import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainIdDbMigrator;
import com.apollocurrency.aplwallet.apl.core.chainid.DbInfoExtractor;
import com.apollocurrency.aplwallet.apl.core.chainid.H2DbInfoExtractor;
import com.apollocurrency.aplwallet.apl.core.db.FullTextTrigger;
import com.apollocurrency.aplwallet.apl.core.db.model.Option;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author al
 */
public class DbMigratorTask {
    private static Logger LOG = LoggerFactory.getLogger(DbMigratorTask.class);
     // TODO: YL remove static instance later
    private static AplGlobalObjects aplGlobalObjects;// = CDI.current().select(AplGlobalObjects.class).get();
    
    private DbMigratorTask() {
    }
    
    public static DbMigratorTask getInstance() {
        if (aplGlobalObjects == null) {
            aplGlobalObjects = CDI.current().select(AplGlobalObjects.class).get();
        }        
        return DbMigratorHolder.INSTANCE;
    }
    
    private static class DbMigratorHolder {

        private static final DbMigratorTask INSTANCE = new DbMigratorTask();
    }

        public void migrateDb() {
            String secondDbMigrationRequired = Option.get("secondDbMigrationRequired");
            boolean secondMigrationRequired = secondDbMigrationRequired == null || Boolean.parseBoolean(secondDbMigrationRequired);
            if (secondMigrationRequired) {
                Option.set("secondDbMigrationRequired", "true");
                LOG.debug("Db migration required");
                Db.shutdown();
                String dbDir = aplGlobalObjects.getStringProperty(Db.PREFIX + "Dir");
                String targetDbDir = AplCoreRuntime.getInstance().getDbDir(dbDir);
                String dbName = aplGlobalObjects.getStringProperty(Db.PREFIX + "Name");
                String dbUser = aplGlobalObjects.getStringProperty(Db.PREFIX + "Username");
                String dbPassword = aplGlobalObjects.getStringProperty(Db.PREFIX + "Password");
                String legacyDbDir = AplCoreRuntime.getInstance().getDbDir(dbDir, null, false);
                String chainIdDbDir = AplCoreRuntime.getInstance().getDbDir(dbDir, true);
                DbInfoExtractor dbInfoExtractor = new H2DbInfoExtractor(dbName, dbUser, dbPassword);
                com.apollocurrency.aplwallet.apl.core.chainid.DbMigrator dbMigrator = new ChainIdDbMigrator(chainIdDbDir, legacyDbDir, dbInfoExtractor);
                try {
                    AppStatus.getInstance().update("Performing database migration");
                    Path oldDbPath = dbMigrator.migrate(targetDbDir);
                    Db.init();
                    try (Connection connection = Db.getDb().getConnection()) {
                        FullTextTrigger.reindex(connection);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                    AplGlobalObjects.createBlockDb(new ConnectionProviderImpl());
                    Option.set("secondDbMigrationRequired", "false");
                    boolean deleteOldDb = aplGlobalObjects.getBooleanProperty("apl.deleteOldDbAfterMigration");
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

        public void performDbMigrationCleanup() {
            String dbDir = aplGlobalObjects.getStringProperty(Db.PREFIX + "Dir");
            String targetDbDir = AplCoreRuntime.getInstance().getDbDir(dbDir);
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
