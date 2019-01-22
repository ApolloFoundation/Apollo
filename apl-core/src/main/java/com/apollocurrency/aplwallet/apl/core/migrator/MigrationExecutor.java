/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MigrationExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationExecutor.class);

    private static final String MIGRATION_REQUIRED_TEMPLATE = "%sMigrationRequired-%d";
    private static final String DELETE_AFTER_MIGRATION_TEMPLATE = "apl.migrator.%s.deleteAfterMigration";
    private static final int ATTEMPT = 0;

    protected PropertiesHolder holder;
    protected BlockchainConfig config;
    private String migrationRequiredPropertyName;
    private String deleteAfterMigrationPropertyName;
    protected OptionDAO optionDAO;
    private String migrationItemName;

    public MigrationExecutor(PropertiesHolder holder, BlockchainConfig config, String migrationItemName) {
        Objects.requireNonNull(holder, "Properties holder cannot be null");
        Objects.requireNonNull(config, "Blockchain config cannot be null");
        if (StringUtils.isBlank(migrationItemName)) {
            throw new IllegalArgumentException("Option prefix cannot be null or blank");
        }
        this.holder = holder;
        this.config = config;
        this.migrationRequiredPropertyName = String.format(MIGRATION_REQUIRED_TEMPLATE, migrationItemName, ATTEMPT);
        this.deleteAfterMigrationPropertyName = String.format(DELETE_AFTER_MIGRATION_TEMPLATE, migrationItemName);
        this.optionDAO = new OptionDAO(Db.getDb());
        this.migrationItemName = migrationItemName;
    }

    protected abstract List<Path> createSrcPaths();

    public void performMigration(Path toPath) throws IOException {
        if (isMigrationRequired()) {
            optionDAO.set(migrationRequiredPropertyName, "true");
            LOG.info("Migration of the {} required", migrationItemName);
            List<Path> listFromPaths = createSrcPaths();
            LOG.debug("Found {} possible migration candidates", listFromPaths.size());
            beforeMigration();
            List<Path> migratedPaths = getMigrator().migrate(listFromPaths, toPath);
            afterMigration();
            optionDAO.set(migrationRequiredPropertyName, "false");
            if (migratedPaths != null && !migratedPaths.isEmpty()) {
                LOG.info("{} migrated successfully", migrationItemName);
                if (isCleanupRequired()) {
                    for (Path migratedPath : migratedPaths) {
                        FileUtils.deleteDirectory(migratedPath.toFile());
                    }
                }
            } else {
                LOG.info("No {} to migrate", migrationItemName);
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
        return parseBooleanProperty(deleteAfterMigrationPropertyName, true);
    }

    private boolean parseBooleanProperty(String property, boolean defaultValue) {
        String propertyString = optionDAO.get(property);
        if (propertyString == null) {
            return defaultValue;
        }
        return (Boolean.parseBoolean(propertyString));
    }

}
