/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.migrator.db;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.FullTextTrigger;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbMigrationExecutor extends MigrationExecutor {
    private static Logger LOG = LoggerFactory.getLogger(DbMigrationExecutor.class);
    private String oldDbPrefix;

    @Inject
    public DbMigrationExecutor(BlockchainConfig config, PropertiesHolder propertiesHolder) {
        super(propertiesHolder, config, "db");
        this.oldDbPrefix = config.isTestnet() ? "apl.testDb" : "apl.db";
    }

    @Override
    protected void afterMigration() {
        Db.init();
        FullTextTrigger.init();
        try (Connection connection = Db.getDb().getConnection()) {
            FullTextTrigger.reindex(connection);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    protected void beforeMigration() {
        Db.shutdown();
    }

    @Override
    protected List<Path> createSrcPaths() {
        LegacyDbLocationsProvider legacyDbLocationsProvider = CDI.current().select(LegacyDbLocationsProvider.class).get();
        return legacyDbLocationsProvider.getDbLocations();
    }

    @Override
    protected Migrator getMigrator() {
        String dbUser = holder.getStringProperty(oldDbPrefix + "Username");
        String dbPassword = holder.getStringProperty(oldDbPrefix + "Password");
        DbInfoExtractor dbInfoExtractor = new H2DbInfoExtractor(dbUser, dbPassword);
        return new DbMigrator(dbInfoExtractor);
    }
}
