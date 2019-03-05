/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.migrator.db;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
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
    private FullTextSearchService fullTextSearchProvider;
    private DbInfoExtractor dbInfoExtractor;
    private DatabaseManager databaseManager;

    @Inject
    public DbMigrationExecutor(PropertiesHolder propertiesHolder, LegacyDbLocationsProvider dbLocationsProvider,
                               DbInfoExtractor dbInfoExtractor, DatabaseManager databaseManager, FullTextSearchService fullTextSearchProvider) {
        super(propertiesHolder, databaseManager, "db", true);
        this.legacyDbLocationsProvider = Objects.requireNonNull(dbLocationsProvider, "Legacy db locations provider cannot be null");
        this.dbInfoExtractor = Objects.requireNonNull(dbInfoExtractor, "Db info extractor cannot be null");
        this.fullTextSearchProvider = Objects.requireNonNull(fullTextSearchProvider, "Fulltext search service cannot be null");
        this.dbInfoExtractor = Objects.requireNonNull(dbInfoExtractor, "Db info extractor cannot be null");
        this.databaseManager = databaseManager;
    }

    @Override
    protected void afterMigration() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        fullTextSearchProvider.init();
        try (Connection connection = dataSource.getConnection()) {
            fullTextSearchProvider.reindexAll(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    protected void beforeMigration() {
        fullTextSearchProvider.shutdown();
        databaseManager.shutdown();
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
