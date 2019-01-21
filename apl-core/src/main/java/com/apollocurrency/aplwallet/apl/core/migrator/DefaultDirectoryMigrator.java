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

public class DefaultDirectoryMigrator implements Migrator {
    @Override
    public List<Path> migrate(List<Path> srcPaths, Path destPath) throws IOException {
        List<Path> listOfMigratedSrcPaths = new ArrayList<>();
        for (Path p : srcPaths) {
            if (Files.isDirectory(p)) {
                listOfMigratedSrcPaths.add(p);
                if (Files.list(p).count() > 0) {
                    FileUtils.copyDirectory(p.toFile(), destPath.toFile());
                }
            }
        }
        return listOfMigratedSrcPaths;
    }
}
