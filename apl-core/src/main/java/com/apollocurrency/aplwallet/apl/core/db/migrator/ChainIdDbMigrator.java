/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.migrator;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ChainIdDbMigrator implements DbMigrator {
    private final List<Path> dbPaths;
    private final DbInfoExtractor dbInfoExtractor;

    private static final Logger LOG = getLogger(ChainIdDbMigrator.class);

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

    public ChainIdDbMigrator(List<Path> dbPaths, DbInfoExtractor dbInfoExtractor) {
        this.dbPaths = dbPaths;
        this.dbInfoExtractor = dbInfoExtractor;
    }


    public DbInfo getOldDbInfo() {
        for (Path dbPath : dbPaths) {
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

    @Override
    public Path migrate(String targetDbPath) throws IOException {
        DbInfo oldDbInfo = getOldDbInfo();
        if (oldDbInfo != null) {
            LOG.info("Found old db for migration at path {}", oldDbInfo.dbPath);
            int height = oldDbInfo.height;
            if (height > 0) {
                Path targetDbFullPath = dbInfoExtractor.getPath(targetDbPath);
                LOG.info("Db {} has blocks - {}. Do migration to {}", oldDbInfo.dbPath, height, targetDbFullPath);
                Files.copy(oldDbInfo.dbPath, targetDbFullPath, StandardCopyOption.REPLACE_EXISTING);
            }
            int actualDbHeight = dbInfoExtractor.getHeight(targetDbPath);
            if (actualDbHeight != height) {
                throw new RuntimeException(String.format("Db was migrated with errors. Expected height - %d, actual - %d. Application restart is " +
                        "needed.", height, actualDbHeight));
            }
            return oldDbInfo.dbDir;
        } else {
            LOG.info("Nothing to migrate");
            return null;
        }
    }
}

