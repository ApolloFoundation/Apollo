/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents migration of application data
 */
public interface Migrator {
    /**
     * Migrate application data from list of paths to target path
     *
     * @param srcPaths list of paths where data for migration stored sorted by importance descending
     * @param destPath path to the target data location, where migration should be performed
     * @return list of migrated data paths or null when migration was not performed
     * @throws IOException when IO error occurred
     */
    List<Path> migrate(List<Path> srcPaths, Path destPath) throws IOException;
}
