/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;

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
 */
public abstract class MigrationExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationExecutor.class);

    private static final String MIGRATION_REQUIRED_TEMPLATE = "%sMigrationRequired-%d";
    private static final String DELETE_AFTER_MIGRATION_TEMPLATE = "apl.migrator.%s.deleteAfterMigration";
    private static final String DO_MIGRATION_TEMPLATE = "apl.migrator.%s.migrate";
    private static final int ATTEMPT = 0;
    private DatabaseManager databaseManager;

    protected PropertiesHolder holder;
    protected BlockchainConfig config;
    private String migrationRequiredPropertyName;
    private String doMigrationPropertyName;
    private String deleteAfterMigrationPropertyName;
    private String migrationItemName;
    private boolean autoCleanup;

    //    set up by perfomMigration method to perform cleanup in future
    private List<Path> migratedPaths;

    public MigrationExecutor(PropertiesHolder holder, DatabaseManager databaseManager, String migrationItemName, boolean autoCleanup) {
        Objects.requireNonNull(holder, "Properties holder cannot be null");
        StringValidator.requireNonBlank(migrationItemName, "Option prefix cannot be null or blank");
        Objects.requireNonNull(databaseManager, " Database manager cannot be null");
        this.autoCleanup = autoCleanup;
        this.holder = holder;
        this.migrationRequiredPropertyName = String.format(MIGRATION_REQUIRED_TEMPLATE, migrationItemName, ATTEMPT);
        this.deleteAfterMigrationPropertyName = String.format(DELETE_AFTER_MIGRATION_TEMPLATE, migrationItemName);
        this.doMigrationPropertyName = String.format(DO_MIGRATION_TEMPLATE, migrationItemName);
        this.migrationItemName = migrationItemName;
        this.databaseManager = databaseManager;
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
                    performAfterMigrationCleanup(toPath);
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

    public void performAfterMigrationCleanup(Path targetPath) throws IOException {
        if (migratedPaths != null) {
            if (isCleanupRequired()) {
                for (Path migratedPath : migratedPaths) {
                      delete(migratedPath, targetPath);
                }
            }
        }
    }

    private void delete(Path path, Path excluded) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.startsWith(excluded)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!excluded.startsWith(dir)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    protected abstract Migrator getMigrator();

    protected void afterMigration() {}

    protected void beforeMigration() {}

    private boolean isMigrationRequired() {
        return parseBooleanProperty(migrationRequiredPropertyName, true) && holder.getBooleanProperty(doMigrationPropertyName, true);
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
