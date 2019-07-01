/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.enterprise.inject.Vetoed;

@Vetoed
public class FileUtils {
    private static final Logger log = getLogger(FileUtils.class);

    public static boolean deleteFileIfExistsQuietly(Path file) {
        try {
            return Files.deleteIfExists(file);
        }
        catch (IOException e) {
            log.error("Unable to delete file " + file, e);
        }
        return false;
    }

    public static boolean deleteFileIfExistsAndHandleException(Path file, Consumer<IOException> handler) {
        try {
            return Files.deleteIfExists(file);
        }
        catch (IOException e) {
            handler.accept(e);
        }
        return false;
    }

    private FileUtils() {}
}
