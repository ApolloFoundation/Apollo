/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.migrator.db;

import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.FullTextTrigger;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * <p>Provide database specific components for migration, also add special {@link DbMigrationExecutor#afterMigration} and
 * {@link DbMigrationExecutor#beforeMigration} handlers
 * to interact with db</p>
 * @see MigrationExecutor
 * @see LegacyDbLocationsProvider
 * @see DbMigrator
 */
public class DbMigrationExecutor extends MigrationExecutor {

    @Inject
    public DbMigrationExecutor(BlockchainConfig config, PropertiesHolder propertiesHolder) {
        super(propertiesHolder, config, "db", true);
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
    protected List<Path> getSrcPaths() {
        LegacyDbLocationsProvider legacyDbLocationsProvider = CDI.current().select(LegacyDbLocationsProvider.class).get();
        return legacyDbLocationsProvider.getDbLocations();
    }

    @Override
    protected Migrator getMigrator() {
        DbInfoExtractor dbInfoExtractor = CDI.current().select(DbInfoExtractor.class).get();
        return new DbMigrator(dbInfoExtractor);
    }
}
