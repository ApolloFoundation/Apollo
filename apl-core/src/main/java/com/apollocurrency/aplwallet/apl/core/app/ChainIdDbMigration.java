/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.core.db.model.Option;
import org.slf4j.Logger;

public class ChainIdDbMigration {
    private static final Logger LOG = getLogger(ChainIdDbMigration.class);
    private static class DbInfo {
        private Path path;
        private int height;

        public DbInfo(Path path, int height) {
            this.path = path;
            this.height = height;
        }
    }
    public static DbInfo getOldDbInfo() throws IOException {
        String dbDir = AplCore.getStringProperty(Db.PREFIX + "Dir");
        String dbName = AplCore.getStringProperty(Db.PREFIX + "Name");

        DbInfo dbInfo = getOldChainIdDbInfo(dbDir, dbName, true);
        if (dbInfo != null) {
            return dbInfo;
        }
        return getLegacyDbInfo(dbDir, dbName, false);
    }

    private static int checkDb(String dbDir, String dbName) {
        String dbUrl = createDbUrl(dbDir, dbName);
        Db.init(dbUrl);
        int height = getHeight();
        Db.shutdown();
        return height;
    }

    private static DbInfo getOldChainIdDbInfo(String dbDir, String dbName, boolean removeWhenNoBlocks) throws IOException {
        String chainIdMigratedDbPath = AplCore.getDbDir(dbDir, true);
        Path path = Paths.get(chainIdMigratedDbPath).normalize();
        int height = checkDb(path.toAbsolutePath().toString(), dbName);
        if (height == 0 && removeWhenNoBlocks) {
            Db.removeDb(path.getParent());
        }
        if (height != 0) {
            return new DbInfo(Paths.get(chainIdMigratedDbPath, dbName), height);
        } else {
            return null;
        }
    }

    private static DbInfo getLegacyDbInfo(String dbDir, String dbName, boolean removeWhenNoBlocks) throws IOException {
        String legacyDbPath = AplCore.getDbDir(dbDir, null, false);
        int height = checkDb(legacyDbPath, dbName);
        if (height == 0 && removeWhenNoBlocks) {
            Db.removeDb(Paths.get(legacyDbPath));
        }
        if (height != 0) {
            return new DbInfo(Paths.get(legacyDbPath, dbName), height);
        } else {
            return null;
        }
    }

    private static int getHeight() {
        BlockImpl lastBlock = BlockDb.findLastBlock();
        return lastBlock == null ? 0 : lastBlock.getHeight();
    }


    static void migrate() throws IOException {
        String secondDbMigrationRequired = Option.get("secondDbMigrationRequired");
        boolean secondMigrationRequired = secondDbMigrationRequired == null || Boolean.parseBoolean(secondDbMigrationRequired);
        if (secondMigrationRequired) {
            Db.shutdown();
            DbInfo oldDbInfo = getOldDbInfo();
            if (oldDbInfo != null) {
                int height = oldDbInfo.height;
                if (height > 0) {
                    LOG.info("Required db migration for applying chainId");
                    String targetDbDir = AplCore.getDbDir(AplCore.getStringProperty(Db.PREFIX + "Dir"));
                    Path targetDbDirPath = Paths.get(targetDbDir);
                    LOG.info("Copying db from {} to {}", oldDbInfo, targetDbDir);
                    copyDir(oldDbInfo.path, targetDbDirPath);

                }
                Db.init();
                if (height > 0) {
                    BlockImpl lb = BlockDb.findLastBlock();
                    int actualHeight = lb == null ? 0 : lb.getHeight();
                    if (actualHeight != height) {
                        LOG.error("Db was copied with errors. Restart your wallet.");
                        System.exit(-1);
                    }
                }
            } else {
                Db.init();
            }
            Option.set("secondDbMigrationRequired", "false");
        }
    }

    private static String createDbUrl(String dbDir, String dbName) {
        return String.format("jdbc:%s:%s", AplCore.getStringProperty(Db.PREFIX + "Type"), dbDir + "/" + dbName);
    }

    private static void copyDir(Path sourceDir, Path targetDir) {
        String chainId = AplGlobalObjects.getChainConfig().getChain().getChainId().toString();
        try {
            List<Path> listOfPathsWithoutTargetDir = Files.walk(sourceDir.getParent()).filter(path -> {
                Path relative = sourceDir.getParent().relativize(path);
                int chainIdIndex = relative.toAbsolutePath().toString().indexOf(chainId);
                return chainIdIndex == -1;
            }).collect(Collectors.toList());

            listOfPathsWithoutTargetDir.forEach(source -> {
                        try {
                            if (Files.isDirectory(source)) {
                                Path targetPath = targetDir.resolve(sourceDir.getParent().relativize(source));
                                if (Files.notExists(targetPath)) {
                                    Files.createDirectory(targetPath);
                                }
                            } else {
                                Files.copy(source, targetDir.resolve(sourceDir.getParent().relativize(source)), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e.toString(), e);
                        }
                    });
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
