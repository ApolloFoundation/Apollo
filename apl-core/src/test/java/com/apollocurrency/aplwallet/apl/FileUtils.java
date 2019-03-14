/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    public static TemporaryFolderExtension initTempFolder() {
        TemporaryFolderExtension temporaryFolder = new TemporaryFolderExtension();
        try {
            temporaryFolder.create();
            return temporaryFolder;
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    public static Path newTempFilePath(TemporaryFolder temporaryFolder, String name) {
            return temporaryFolder.getRoot().toPath().resolve(name);
    }

    public static Path createTempFile(String name) {
        try {
            return Files.createTempFile(name, "");
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


}
