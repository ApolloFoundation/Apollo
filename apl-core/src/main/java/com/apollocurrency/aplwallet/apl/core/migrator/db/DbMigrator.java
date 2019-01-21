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
            if (height > 0) {
                Path targetDbFullPath = dbInfoExtractor.getPath(toPath.toAbsolutePath().toString());
                LOG.info("Db {} has blocks - {}. Do migration to {}", oldDbInfo.dbPath, height, targetDbFullPath);
                Files.copy(oldDbInfo.dbPath, targetDbFullPath, StandardCopyOption.REPLACE_EXISTING);
            }
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
        this.dbInfoExtractor = dbInfoExtractor;
    }


    public DbInfo getOldDbInfo(List<Path> paths) {
        for (Path dbPath : paths) {
            int height = dbInfoExtractor.getHeight(dbPath.toAbsolutePath().toString());
            if (height != 0) {
                return new DbInfo(dbInfoExtractor.getPath(dbPath.toAbsolutePath().toString()), dbPath.getParent(), height);
            }
        }
        return null;
    }

//    private Path getRootDbDir(Path dbPath, Path currentDbPath) {
//        Path rootDbRoot = dbPath;
//        while (!currentDbPath.startsWith(rootDbRoot.getParent())) {
//            rootDbRoot = rootDbRoot.getParent();
//        }
//        return rootDbRoot;
//    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation check two database locations specified by <b>chainIdDbDir</b> and <b>legacyDbDir</b>.
     * Database at <b>chainIdDbDir</b> is preferred.
     * Migration will be performed only when at least one database exists at specified locations and has blocks. In such case old path of the
     * migrated database will be returned, otherwise null will be returned
     * </p>
     */

}

