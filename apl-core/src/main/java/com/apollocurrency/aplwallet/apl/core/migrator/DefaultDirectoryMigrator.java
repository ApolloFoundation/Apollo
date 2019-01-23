/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

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
        List<Path> listOfMigratedSrcPaths = new ArrayList<>();
        if (Files.isDirectory(destDirectoryPath)) {
            for (Path p : srcDirectoriesPaths) {
                if (Files.exists(p)) {
                    if (Files.isDirectory(p)) {
                        if (Files.list(p).count() > 0) {
                            listOfMigratedSrcPaths.add(p);
                            FileUtils.copyDirectory(p.toFile(), destDirectoryPath.toFile());
                        }
                    } else throw new IllegalArgumentException("List of src directories should contain only directories");
                }
            }
        } else throw new IllegalArgumentException("Destionation path is not a directory");
        return listOfMigratedSrcPaths;
    }
}
