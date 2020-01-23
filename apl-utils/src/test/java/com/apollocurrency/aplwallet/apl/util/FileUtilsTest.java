/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testClearDirectory() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file1.txt"));
        Files.createFile(directory.resolve("file2.txt"));
        FileUtils.clearDirectorySilently(directory);
        assertTrue(Files.exists(directory));
        assertEquals(0, Files.list(directory).count());
    }

    @Test
    void testDeleteWithSuffix() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file1.tx"));
        Files.createFile(directory.resolve("file2.txt"));
        Files.createFile(directory.resolve("file3.tt"));
        FileUtils.deleteFilesByPattern(directory, new String[]{"tx", "txt"}, null);
        assertTrue(Files.exists(directory));
        assertEquals(1, Files.list(directory).count());
    }

    @Test
    void testDeleteWithName() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file1.tx"));
        Files.createFile(directory.resolve("filex2.txt"));
        Path shouldExists = Files.createFile(directory.resolve("fil3.tt"));
        FileUtils.deleteFilesByPattern(directory, null, new String[]{"file"});
        assertTrue(Files.exists(directory));
        assertEquals(1, Files.list(directory).count());
        assertTrue(Files.exists(shouldExists));
    }

    @Test
    void testDeleteByNameAndSuffix() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file1.tt"));
        Files.createFile(directory.resolve("filex2.txt"));
        Files.createFile(directory.resolve("filex4.tt"));
        Files.createFile(directory.resolve("test5.txt"));
        Files.createFile(directory.resolve("fil3.tt"));
        FileUtils.deleteFilesByPattern(directory, new String[]{"tt"}, new String[]{"file"});
        assertTrue(Files.exists(directory));
        assertEquals(3, Files.list(directory).count());
    }

    @Test
    void testDeletebyFilter() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file1.txt"));
        Files.createFile(directory.resolve("file2.txt"));
        Path existingFile = directory.resolve("file3.tt");
        Files.createFile(existingFile);
        FileUtils.deleteFilesByFilter(directory, (p) -> p.toString().endsWith(".txt"));
        long filesCount = Files.list(directory).count();
        assertEquals(1, filesCount);
        assertTrue(Files.exists(existingFile));
    }
}