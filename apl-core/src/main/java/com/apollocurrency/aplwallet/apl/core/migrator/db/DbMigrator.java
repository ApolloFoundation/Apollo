/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

/**
 * Implement db migration specific algorithm
 */
public class DbMigrator  {
/*    private static final Logger LOG = getLogger(DbMigrator.class);
    private final DbInfoExtractor dbInfoExtractor;

    public DbMigrator(DbInfoExtractor dbInfoExtractor) {
        Objects.requireNonNull(dbInfoExtractor, "Db info extractor cannot be null");
        this.dbInfoExtractor = dbInfoExtractor;
    }

    *//**
     * {@inheritDoc}
     * <p><br>This implementation migrate db (copy db file) from one of the srcPaths file to destPath. Migration will be performed only when any path
     * from srcPaths will represent blockchain db and has height of blocks > 0</p>
     *//*
    @Override
    public List<Path> migrate(List<Path> srcPaths, Path destPath) throws IOException {
        List<Path> migratedDbsPaths = new ArrayList<>();
        DbInfo oldDbInfo = getOldDbInfo(srcPaths);
        if (oldDbInfo != null) {
            LOG.info("Found old db for migration at path {}", oldDbInfo.dbPath);
            int height = oldDbInfo.height;
            Path targetDbFullPath = dbInfoExtractor.getPath(destPath.toAbsolutePath().toString());
            LOG.info("Db {} has blocks - {}. Do migration to {}", oldDbInfo.dbPath, height, targetDbFullPath);

            Files.copy(oldDbInfo.dbPath, targetDbFullPath, StandardCopyOption.REPLACE_EXISTING);
            int actualDbHeight = dbInfoExtractor.getHeight(destPath.toAbsolutePath().toString());
            if (actualDbHeight != height) {
                throw new RuntimeException(String.format("Db was migrated with errors. Expected height - %d, actual - %d. Application restart is " +
                    "needed.", height, actualDbHeight));
            }
            migratedDbsPaths.add(oldDbInfo.dbDir);
        }
        return migratedDbsPaths;
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

    private static class DbInfo {
        private Path dbPath;
        private Path dbDir;
        private int height;

        public DbInfo(Path dbPath, Path dbDir, int height) {
            this.dbPath = dbPath;
            this.dbDir = dbDir;
            this.height = height;
        }
    }*/
}

