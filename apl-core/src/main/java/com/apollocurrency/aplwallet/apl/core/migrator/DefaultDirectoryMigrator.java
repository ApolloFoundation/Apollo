/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Migrate files from list directories to the target directory using simple recursive copying
 */
public class DefaultDirectoryMigrator implements Migrator {
    /**
     * Copy data from srcPaths directores to destDirectoryPath directory recursively
     * @param srcDirectoriesPaths list of directories where data for migration stored
     * @param destDirectoryPath path to the target directory, where migration should be performed
     * @return list of directories files from which were migrated (empty directores will be ignored)
     * @throws IOException when IO error occurred
     * @throws IllegalArgumentException when one among srcDirectoriesPaths or destDirectoryPath is not a directory
     */
    @Override
    public List<Path> migrate(List<Path> srcDirectoriesPaths, Path destDirectoryPath) throws IOException {
        Objects.requireNonNull(srcDirectoriesPaths, "Src directories should not be null");
        Objects.requireNonNull(destDirectoryPath, "Dest directory should not be null");

        List<Path> listOfMigratedSrcPaths = new ArrayList<>();
        if (!Files.exists(destDirectoryPath) || Files.isDirectory(destDirectoryPath) && Files.exists(destDirectoryPath)) {
            Path tempDirectory = Files.createTempDirectory("migration-temp-dir");
            try {
                boolean migrated = false;
                for (Path p : srcDirectoriesPaths) {
                    if (Files.exists(p)) {
                        if (Files.isDirectory(p)) {
                            if (com.apollocurrency.aplwallet.apl.util.FileUtils.countElementsOfDirectory(p) > 0) {
                                if (!p.equals(destDirectoryPath)) {
                                    listOfMigratedSrcPaths.add(p);
                                    FileUtils.copyDirectory(p.toFile(), tempDirectory.toFile());
                                    migrated = true;
                                }
                            }
                        } else throw new IllegalArgumentException("List of src directories should contain only directories");
                    }
                }
                if (migrated) {
                    FileUtils.copyDirectory(tempDirectory.toFile(), destDirectoryPath.toFile());
                }
            }finally {
                FileUtils.deleteDirectory(tempDirectory.toFile());

            }
        } else throw new IllegalArgumentException("Destionation path is not a directory");
        return listOfMigratedSrcPaths;
    }
}
