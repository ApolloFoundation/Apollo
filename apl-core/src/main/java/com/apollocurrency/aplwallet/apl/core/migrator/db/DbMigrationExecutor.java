/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.migrator.db;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.inject.Inject;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * <p>Provide database specific components for migration, also add special {@link DbMigrationExecutor#afterMigration} and
 * {@link DbMigrationExecutor#beforeMigration} handlers
 * to interact with db</p>
 *
 * @see MigrationExecutor
 * @see LegacyDbLocationsProvider
 * @see DbMigrator
 */
public class DbMigrationExecutor  {
/*    private LegacyDbLocationsProvider legacyDbLocationsProvider;
    private FullTextSearchService fullTextSearchProvider;
    private DbInfoExtractor dbInfoExtractor;
    private DatabaseManager databaseManager;
    private JdbiHandleFactory jdbiHandleFactory;

    @Inject
    public DbMigrationExecutor(PropertiesHolder propertiesHolder, LegacyDbLocationsProvider dbLocationsProvider,
                               DbInfoExtractor dbInfoExtractor, DatabaseManager databaseManager, FullTextSearchService fullTextSearchProvider, JdbiHandleFactory jdbiHandleFactory) {
        super(propertiesHolder, databaseManager, "db", true);
        this.legacyDbLocationsProvider = Objects.requireNonNull(dbLocationsProvider, "Legacy db locations provider cannot be null");
        this.dbInfoExtractor = Objects.requireNonNull(dbInfoExtractor, "Db info extractor cannot be null");
        this.fullTextSearchProvider = Objects.requireNonNull(fullTextSearchProvider, "Fulltext search service cannot be null");
        this.dbInfoExtractor = Objects.requireNonNull(dbInfoExtractor, "Db info extractor cannot be null");
        this.databaseManager = databaseManager;
        this.jdbiHandleFactory = Objects.requireNonNull(jdbiHandleFactory, "Jdbi handle factory cannot be null");
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
        jdbiHandleFactory.setJdbi(databaseManager.getJdbi());
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
    }*/
}
