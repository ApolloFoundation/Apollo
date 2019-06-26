/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

class FileUtilsTest {
    @RegisterExtension
    TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    @Test
    void deleteFileIfExistsQuietly() throws IOException {
        Path file = temporaryFolderExtension.newFile("file").toPath();
        FileUtils.deleteFileIfExistsQuietly(file);
        assertFalse(Files.exists(file));
    }

    @Test
    void deleteFileIfExistsWithException() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file.txt"));
        FileUtils.deleteFileIfExistsQuietly(directory);
        assertTrue(Files.exists(directory));
    }

    @Test
    void deleteFileIfExistsAndHandleException() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file.txt"));
        boolean result = FileUtils.deleteFileIfExistsAndHandleException(directory, (e) -> assertEquals(DirectoryNotEmptyException.class, e.getClass()));
        assertFalse(result);
        assertTrue(Files.exists(directory));
    }
}