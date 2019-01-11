/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

public class ChainIdDbMigrator implements DbMigrator {
    private final String chainIdDbDir;
    private final String legacyDbDir;
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

    public ChainIdDbMigrator(String chainIdDbDir, String legacyDbDir, DbInfoExtractor dbInfoExtractor) {
        this.chainIdDbDir = chainIdDbDir;
        this.legacyDbDir = legacyDbDir;
        this.dbInfoExtractor = dbInfoExtractor;
    }


    public DbInfo getOldDbInfo() {

        int chainIdDbHeight = dbInfoExtractor.getHeight(chainIdDbDir);
        if (chainIdDbHeight != 0) {
            return new DbInfo(dbInfoExtractor.getPath(chainIdDbDir), Paths.get(chainIdDbDir).getParent(), chainIdDbHeight);
        }

        int legacyDbHeight = dbInfoExtractor.getHeight(legacyDbDir);
        if (legacyDbHeight != 0) {

            return new DbInfo(dbInfoExtractor.getPath(legacyDbDir), Paths.get(legacyDbDir), legacyDbHeight);
        } else return null;
    }

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
    public Path migrate(String targetDbDir) throws IOException {
        DbInfo oldDbInfo = getOldDbInfo();
        if (oldDbInfo != null) {
            LOG.info("Found old db for migration at path {}", oldDbInfo.dbPath);
            int height = oldDbInfo.height;
            if (height > 0) {
                LOG.info("Db {} has blocks - {}. Do migration to {}", oldDbInfo.dbPath, height, targetDbDir);
                Path targetDbDirPath = dbInfoExtractor.getPath(targetDbDir);
                Files.copy(oldDbInfo.dbPath, targetDbDirPath, StandardCopyOption.REPLACE_EXISTING);
            }
            int actualDbHeight = dbInfoExtractor.getHeight(targetDbDir);
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

    public static void deleteAllWithExclusion(Path pathToDelete, Path pathToExclude) throws IOException {

        List<Path> excludedPaths = Files.walk(pathToExclude.normalize()).collect(Collectors.toList());
        if (pathToExclude.startsWith(pathToDelete)) {
            Path relativePath = pathToDelete.relativize(pathToExclude);
            for (Path aRelativePath : relativePath) {
                excludedPaths.add(aRelativePath);
            }
            excludedPaths.add(pathToDelete.normalize());
        }
        Files.walkFileTree(pathToDelete.normalize(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!excludedPaths.contains(file)) {
                    Files.delete(file);
                }
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!excludedPaths.contains(dir)) {
                    Files.delete(dir);
                }
                return super.postVisitDirectory(dir, exc);
            }
        });
    }
}

