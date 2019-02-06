/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.migrator.db;

import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.db.FullTextTrigger;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

/**
 * <p>Provide database specific components for migration, also add special {@link DbMigrationExecutor#afterMigration} and
 * {@link DbMigrationExecutor#beforeMigration} handlers
 * to interact with db</p>
 * @see MigrationExecutor
 * @see LegacyDbLocationsProvider
 * @see DbMigrator
 */
public class DbMigrationExecutor extends MigrationExecutor {
    private LegacyDbLocationsProvider legacyDbLocationsProvider;
    private DbInfoExtractor dbInfoExtractor;
    private DbProperties dbProperties;// it should be present and initialized

    @Inject
    public DbMigrationExecutor(PropertiesHolder propertiesHolder, LegacyDbLocationsProvider dbLocationsProvider,
                               DbInfoExtractor dbInfoExtractor, DbProperties dbProperties) {
        super(propertiesHolder, "db", true);
        this.legacyDbLocationsProvider = Objects.requireNonNull(dbLocationsProvider, "Legacy db locations provider cannot be null");
        this.dbInfoExtractor = Objects.requireNonNull(dbInfoExtractor, "Db info extractor cannot be null");
        this.dbProperties = dbProperties;

    }

    @Override
    protected void afterMigration() {
        Db.init(dbProperties);
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
        return legacyDbLocationsProvider.getDbLocations();
    }

    @Override
    protected Migrator getMigrator() {
        return new DbMigrator(dbInfoExtractor);
    }
}
