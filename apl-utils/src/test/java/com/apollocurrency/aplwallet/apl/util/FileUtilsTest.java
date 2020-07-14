/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        assertEquals(0, FileUtils.countElementsOfDirectory(directory));
    }

    @Test
    void testDeleteWithSuffix() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file1.tx"));
        Files.createFile(directory.resolve("file2.txt"));
        Files.createFile(directory.resolve("file3.tt"));
        FileUtils.deleteFilesByPattern(directory, new String[]{"tx", "txt"}, null);
        assertTrue(Files.exists(directory));
        assertEquals(1, FileUtils.countElementsOfDirectory(directory));
    }

    @Test
    void testDeleteWithName() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file1.tx"));
        Files.createFile(directory.resolve("filex2.txt"));
        Path shouldExists = Files.createFile(directory.resolve("fil3.tt"));
        FileUtils.deleteFilesByPattern(directory, null, new String[]{"file"});
        assertTrue(Files.exists(directory));
        assertEquals(1, FileUtils.countElementsOfDirectory(directory));
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
        assertEquals(3, FileUtils.countElementsOfDirectory(directory));
    }

    @Test
    void testDeletebyFilter() throws IOException {
        Path directory = temporaryFolderExtension.newFolder().toPath();
        Files.createFile(directory.resolve("file1.txt"));
        Files.createFile(directory.resolve("file2.txt"));
        Path existingFile = directory.resolve("file3.tt");
        Files.createFile(existingFile);
        FileUtils.deleteFilesByFilter(directory, (p) -> p.toString().endsWith(".txt"));
        long filesCount = FileUtils.countElementsOfDirectory(directory);
        assertEquals(1, filesCount);
        assertTrue(Files.exists(existingFile));
    }

    @Test
    void testGetFreeSpace() {
        assertTrue(FileUtils.freeSpace() > 0);
    }

    @Test
    void testGetWebSize() throws IOException {
        File file = temporaryFolderExtension.newFile("file");
        Files.writeString(file.toPath(), "tralala");
        long size = FileUtils.webFileSize("file:///" + file.getAbsolutePath());
        assertEquals(7, size);
    }

    @Test
    void testHashFile() throws IOException, NoSuchAlgorithmException {
        File file = temporaryFolderExtension.newFile("file-to-hash");
        Files.writeString(file.toPath(), "Some content \n to hash \n it \r \n \t");
        byte[] arr = FileUtils.hashFile(file.toPath(), MessageDigest.getInstance("SHA-256"));
        assertEquals("52ae3f0066b74f5ca6d2bb5ccf83340a575e1ef766cb15023a48d1d65865cfd8", Convert.toHexString(arr));
    }
}