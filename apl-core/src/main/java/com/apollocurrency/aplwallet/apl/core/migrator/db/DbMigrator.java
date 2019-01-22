/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
import org.slf4j.Logger;

public class DbMigrator implements Migrator {
    private final DbInfoExtractor dbInfoExtractor;

    private static final Logger LOG = getLogger(DbMigrator.class);

    @Override
    public List<Path> migrate(List<Path> fromPaths, Path toPath) throws IOException {
        List<Path> migratedDbsPaths = new ArrayList<>();
        DbInfo oldDbInfo = getOldDbInfo(fromPaths);
        if (oldDbInfo != null) {
            LOG.info("Found old db for migration at path {}", oldDbInfo.dbPath);
            int height = oldDbInfo.height;
            Path targetDbFullPath = dbInfoExtractor.getPath(toPath.toAbsolutePath().toString());
            LOG.info("Db {} has blocks - {}. Do migration to {}", oldDbInfo.dbPath, height, targetDbFullPath);

            Files.copy(oldDbInfo.dbPath, targetDbFullPath, StandardCopyOption.REPLACE_EXISTING);
            int actualDbHeight = dbInfoExtractor.getHeight(toPath.toAbsolutePath().toString());
            if (actualDbHeight != height) {
                throw new RuntimeException(String.format("Db was migrated with errors. Expected height - %d, actual - %d. Application restart is " +
                        "needed.", height, actualDbHeight));
            }
            migratedDbsPaths.add(oldDbInfo.dbDir);
        }
        return migratedDbsPaths;
    }

    private static class DbInfo {
        private Path dbPath;
        private Path dbDir;
        private int height;

        public DbInfo(Path dbPath, Path dbDir, int height) {
            this.dbPath = dbPath;
            this.dbDir = dbDir;
            this.height = height;
        }
    }

    public DbMigrator(DbInfoExtractor dbInfoExtractor) {
        Objects.requireNonNull(dbInfoExtractor, "Db info extractor cannot be null");
        this.dbInfoExtractor = dbInfoExtractor;
    }


    public DbInfo getOldDbInfo(List<Path> paths) {
        for (Path dbPath : paths) {
            int height = dbInfoExtractor.getHeight(dbPath.toAbsolutePath().toString());
            if (height > 0) {
                return new DbInfo(dbInfoExtractor.getPath(dbPath.toAbsolutePath().toString()), dbPath.getParent(), height);
            }
        }
        return null;
    }
}

