/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.enterprise.inject.spi.CDI;

/**
 * <p>Provides main algorithm of data migration. </p>
 * <p>Should be extended to provide data specific migration components,
 * such as list of paths for migration represented by  {@link MigrationExecutor#getSrcPaths()}
 * and special {@link Migrator} implementation. Also while migration, some logs and db manipulation also
 * require special {@link MigrationExecutor#migrationItemName} to name migration component.</p>
 * <p>For default auto cleanup you can also specify {@link MigrationExecutor#autoCleanup}</p>
 * <p>Typical use case</p>
 * <pre>
 *     class MyDataMigrationExecutor extends MigrationExecutor {
 *         public MyDataMigrationExecutor(PropertiesHolder holder, BlockchainConfig config) {
 *             super(holder, config, "myData", true);
 *         }
 *
 *         protected List<Path> getSrcPaths() {
 *             return Arrays.asList(Paths.get("mydatta"));
 *         }
 *         protected Migrator getMigrator() {
 *             return new MyDataMigrator();
 *         }
 *      }
 * </pre>
 *
 */
public abstract class MigrationExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationExecutor.class);

    private static final String MIGRATION_REQUIRED_TEMPLATE = "%sMigrationRequired-%d";
    private static final String DELETE_AFTER_MIGRATION_TEMPLATE = "apl.migrator.%s.deleteAfterMigration";
    private static final int ATTEMPT = 0;
    private static DatabaseManager databaseManager;

    protected PropertiesHolder holder;
    protected BlockchainConfig config;
    private String migrationRequiredPropertyName;
    private String deleteAfterMigrationPropertyName;
    private String migrationItemName;
    private boolean autoCleanup;

//    set up by perfomMigration method to perform cleanup in future
    private List<Path> migratedPaths;

    public MigrationExecutor(PropertiesHolder holder, DatabaseManager databaseManagerParam, String migrationItemName, boolean autoCleanup) {
        Objects.requireNonNull(holder, "Properties holder cannot be null");
        StringValidator.requireNonBlank(migrationItemName, "Option prefix cannot be null or blank");

        this.autoCleanup = autoCleanup;
        this.holder = holder;
        this.migrationRequiredPropertyName = String.format(MIGRATION_REQUIRED_TEMPLATE, migrationItemName, ATTEMPT);
        this.deleteAfterMigrationPropertyName = String.format(DELETE_AFTER_MIGRATION_TEMPLATE, migrationItemName);
        this.migrationItemName = migrationItemName;
        if (databaseManager == null) {
            if (databaseManagerParam != null) {
                databaseManager = databaseManagerParam;
            } else {
                databaseManager = CDI.current().select(DatabaseManager.class).get();
            }
        }
    }

    public void setAutoCleanup(boolean autoCleanup) {
        this.autoCleanup = autoCleanup;
    }

    protected abstract List<Path> getSrcPaths();

    public void performMigration(Path toPath) throws IOException {
        if (isMigrationRequired()) {
            new OptionDAO(databaseManager).set(migrationRequiredPropertyName, "true");
            LOG.info("Migration of the {} required", migrationItemName);
            List<Path> listFromPaths = getSrcPaths();
            LOG.debug("Found {} possible migration candidates", listFromPaths.size());
            beforeMigration();
            this.migratedPaths = getMigrator().migrate(listFromPaths, toPath);
            afterMigration();
            TransactionalDataSource dataSource = databaseManager.getDataSource(); // retrieve again to refresh
            new OptionDAO(databaseManager).set(migrationRequiredPropertyName, "false");
            if (migratedPaths != null && !migratedPaths.isEmpty()) {
                if (autoCleanup) {
                    performAfterMigrationCleanup();
                }
                LOG.info("{} migrated successfully", migrationItemName);
            } else {
                LOG.info("No {} to migrate", migrationItemName);
            }
        }
    }
    public boolean isAutoCleanup() {
        return autoCleanup;
    }

    public void performAfterMigrationCleanup() throws IOException {
        if (migratedPaths != null) {
            if (isCleanupRequired()) {
                for (Path migratedPath : migratedPaths) {
                    FileUtils.deleteDirectory(migratedPath.toFile());
                }
            }
        }
    }

    protected abstract Migrator getMigrator();

    protected void afterMigration() {}

    protected void beforeMigration() {}

    private boolean isMigrationRequired() {
        return parseBooleanProperty(migrationRequiredPropertyName, true);
    }
    private boolean isCleanupRequired() {
        return holder.getBooleanProperty(deleteAfterMigrationPropertyName, true);
    }

    private boolean parseBooleanProperty(String property, boolean defaultValue) {
        String propertyString = new OptionDAO(databaseManager).get(property);
        if (propertyString == null) {
            return defaultValue;
        }
        return (Boolean.parseBoolean(propertyString));
    }

}
