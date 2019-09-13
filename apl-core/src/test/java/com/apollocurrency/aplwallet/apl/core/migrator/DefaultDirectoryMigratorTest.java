/*
 * Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.migrator;


import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultDirectoryMigratorTest {
    private Path firstDirFile1;
    private Path firstDirFile2;
    private Path firstDir;
    private Path secondFile;
    private Path secondDir;
    private Path secondFirstDir;
    private Path thirdFile;
    private Path thirdDir;
    private Path thirdNestedDir;
    private Path emptyDir;
    private Path noneExistentDir1;
    private Path noneExistentDir2;
    private Path targetFile;
    private Path targetDir;
    private byte[] firstFilePayload = "TestPayload-1".getBytes();
    private byte[] secondFilePayload = "TestPayload-2".getBytes();
    private byte[] thirdFilePayload = "TestPayload-3".getBytes();
    private byte[] targetFilePayload = "TargetPayload".getBytes();
    @RegisterExtension
    TemporaryFolderExtension tmpFolder = new TemporaryFolderExtension();
    @BeforeEach
    public void init() throws IOException {
        firstDir = tmpFolder.newFolder("d-1_").toPath();
        firstDirFile1 = Files.createFile(firstDir.resolve("f-1"));
        Files.write(firstDirFile1, firstFilePayload);
        secondDir = tmpFolder.newFolder("d-2").toPath();
        secondFirstDir = Files.createDirectory(secondDir.resolve("d-2-1"));
        secondFile = Files.createFile(secondDir.resolve("f-2"));
        Files.write(secondFile, secondFilePayload);
        thirdDir = tmpFolder.newFolder("d-3").toPath();
        thirdNestedDir = Files.createDirectory(thirdDir.resolve("d-3_1"));
        thirdFile = Files.createFile(thirdNestedDir.resolve("f-3"));
        Files.write(thirdFile, thirdFilePayload);
        emptyDir = tmpFolder.newFolder("empty").toPath();
        targetDir = tmpFolder.newFolder("target").toPath();
        targetFile = Files.createFile(targetDir.resolve("target-file"));
        Files.write(targetFile, targetFilePayload);
        firstDirFile2 = Files.createFile(firstDir.resolve(targetFile.getFileName()));
        Files.write(firstDirFile2, firstFilePayload);
        noneExistentDir1 = emptyDir.resolve("nonexistentDir1");
        noneExistentDir2 = targetDir.resolve("nonexistentDir2");
    }
    @Test
    public void testMigrateFilesToDirectory() throws IOException {
        List<Path> filesToMigrate = Arrays.asList(firstDirFile1, secondFile);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> defaultDirectoryMigrator.migrate(filesToMigrate, targetDir));
    }

    @Test
    public void testMigrateDirToFile() {
        List<Path> dirsToMigrate = Arrays.asList(secondDir, firstDir);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> defaultDirectoryMigrator.migrate(dirsToMigrate, targetFile));
    }
    @Test
    public void testMigrateFromFileToFile() {
        List<Path> filesToMigrate = Arrays.asList(firstDirFile1);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> defaultDirectoryMigrator.migrate(filesToMigrate, targetFile));
    }
    @Test
    public void testMigrateDirectoryToDirectory() throws IOException {
        List<Path> dirsToMigrate = Arrays.asList(firstDir);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();
        List<Path> migratedDirs = defaultDirectoryMigrator.migrate(dirsToMigrate, targetDir);
        Assertions.assertEquals(migratedDirs, dirsToMigrate);
        Path firstDirFile1TargetPath = targetDir.resolve(firstDirFile1.getFileName());
        Path firstDirFile2TargetPath = targetDir.resolve(firstDirFile2.getFileName());
        Set<Path> expectedMigratedFilePaths = new HashSet<>(Arrays.asList(
                firstDirFile1TargetPath, firstDirFile2TargetPath));
        Assertions.assertEquals(expectedMigratedFilePaths, Files.list(targetDir).collect(Collectors.toSet()));
        Assertions.assertArrayEquals(firstFilePayload, Files.readAllBytes(firstDirFile2TargetPath));
        Assertions.assertArrayEquals(firstFilePayload, Files.readAllBytes(firstDirFile1TargetPath));
    }
    @Test
    public void testMigrateDirectoriesToDirectory() throws IOException {
        List<Path> dirsToMigrate = Arrays.asList(firstDir, secondDir);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();

        Path firstDirFile1TargetPath = targetDir.resolve(firstDirFile1.getFileName());
        Path firstDirFile2TargetPath = targetDir.resolve(firstDirFile2.getFileName());
        Path secondDirFileTargetPath = targetDir.resolve(secondFile.getFileName());
        Path secondFirstDirTargetPath = targetDir.resolve(secondFirstDir.getFileName());
        Set<Path> expectedMigratedFilePaths = new HashSet<>(Arrays.asList(
                firstDirFile1TargetPath, firstDirFile2TargetPath, secondDirFileTargetPath, secondFirstDirTargetPath));

        List<Path> migratedDirs = defaultDirectoryMigrator.migrate(dirsToMigrate, targetDir);

        Assertions.assertEquals(migratedDirs, dirsToMigrate);
        Assertions.assertEquals(expectedMigratedFilePaths, Files.list(targetDir).collect(Collectors.toSet()));
        Assertions.assertArrayEquals(firstFilePayload, Files.readAllBytes(firstDirFile2TargetPath));
        Assertions.assertArrayEquals(firstFilePayload, Files.readAllBytes(firstDirFile1TargetPath));
        Assertions.assertArrayEquals(secondFilePayload, Files.readAllBytes(secondDirFileTargetPath));
    }

    @Test
    public void testMigrateNestedDirectoriesToDirectory() throws IOException {
        List<Path> dirsToMigrate = Arrays.asList(thirdDir);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();

        Path thirdDirNestedDirTargetPath = targetDir.resolve(thirdNestedDir.getFileName());
        Path thirdDirFileTargetPath = thirdDirNestedDirTargetPath.resolve(thirdFile.getFileName());
        Set<Path> expectedMigratedFilePaths = new HashSet<>(Arrays.asList(
                 thirdDirFileTargetPath, thirdDirNestedDirTargetPath, targetFile));

        List<Path> migratedDirs = defaultDirectoryMigrator.migrate(dirsToMigrate, targetDir);

        Assertions.assertEquals(migratedDirs, dirsToMigrate);
        Set<Path> actualFilePaths = Files.walk(targetDir).filter(f -> !f.equals(targetDir)).collect(Collectors.toSet());
        Assertions.assertEquals(expectedMigratedFilePaths, actualFilePaths);
        Assertions.assertArrayEquals(thirdFilePayload, Files.readAllBytes(thirdDirFileTargetPath));
        Assertions.assertArrayEquals(targetFilePayload, Files.readAllBytes(targetFile));
    }

    @Test
    public void testMigrateEmptyDirectory() throws IOException {
        List<Path> dirsToMigrate = Arrays.asList(emptyDir);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();

        Set<Path> expectedMigratedFilePaths = new HashSet<>(Collections.singletonList(
                targetFile));

        List<Path> migratedDirs = defaultDirectoryMigrator.migrate(dirsToMigrate, targetDir);

        Assertions.assertEquals(Collections.emptyList(), migratedDirs);
        Set<Path> actualFilePaths = Files.walk(targetDir).filter(f -> !f.equals(targetDir)).collect(Collectors.toSet());
        Assertions.assertEquals(expectedMigratedFilePaths, actualFilePaths);
        Assertions.assertArrayEquals(targetFilePayload, Files.readAllBytes(targetFile));
    }
    @Test
    public void testMigrateDirectoryToDirectoryWhichNotExist() throws IOException {
        List<Path> dirsToMigrate = Arrays.asList(firstDir);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();

        Path firstDirFile1TargetPath = noneExistentDir1.resolve(firstDirFile1.getFileName());
        Path firstDirFile2TargetPath = noneExistentDir1.resolve(firstDirFile2.getFileName());
        Set<Path> expectedMigratedFilePaths = new HashSet<>(Arrays.asList(
                firstDirFile1TargetPath, firstDirFile2TargetPath));

        Assertions.assertFalse(Files.exists(noneExistentDir1));
        Assertions.assertFalse(Files.isDirectory(noneExistentDir1));

        List<Path> actualMigratedPaths = defaultDirectoryMigrator.migrate(dirsToMigrate, noneExistentDir1);


        Assertions.assertEquals(dirsToMigrate, actualMigratedPaths);
        Set<Path> actualFilePaths = Files.walk(noneExistentDir1).filter(f -> !f.equals(noneExistentDir1)).collect(Collectors.toSet());
        Assertions.assertEquals(expectedMigratedFilePaths, actualFilePaths);
        Assertions.assertArrayEquals(firstFilePayload, Files.readAllBytes(firstDirFile1TargetPath));
        Assertions.assertArrayEquals(firstFilePayload, Files.readAllBytes(firstDirFile2TargetPath));

        Assertions.assertTrue(Files.exists(noneExistentDir1));
        Assertions.assertTrue(Files.isDirectory(noneExistentDir1));

    }
    @Test
    public void testMigrateNonexistentDirectoryDirectory() throws IOException {
        List<Path> dirsToMigrate = Arrays.asList(noneExistentDir1);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();

        List<Path> actualMigratedPaths = defaultDirectoryMigrator.migrate(dirsToMigrate, targetDir);

        Assertions.assertEquals(Collections.emptyList(), actualMigratedPaths);
        Set<Path> actualFilePaths = Files.walk(targetDir).filter(f -> !f.equals(targetDir)).collect(Collectors.toSet());
        Assertions.assertEquals(new HashSet<>(Arrays.asList(targetFile)), actualFilePaths);
        Assertions.assertArrayEquals(targetFilePayload, Files.readAllBytes(targetFile));
    }
    @Test
    public void testMigrateNonexistentDirectoryToNonexistentDirectory() throws IOException {
        List<Path> dirsToMigrate = Arrays.asList(noneExistentDir1);
        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();

        Assertions.assertFalse(Files.exists(noneExistentDir2));
        Assertions.assertFalse(Files.isDirectory(noneExistentDir2));

        List<Path> actualMigratedPaths = defaultDirectoryMigrator.migrate(dirsToMigrate, noneExistentDir2);

        Assertions.assertEquals(Collections.emptyList(), actualMigratedPaths);

        Assertions.assertFalse(Files.exists(noneExistentDir2));
        Assertions.assertFalse(Files.isDirectory(noneExistentDir2));

        Assertions.assertFalse(Files.exists(noneExistentDir1));
        Assertions.assertFalse(Files.isDirectory(noneExistentDir1));
    }

    @Test
    public void testMigrateToInnerDirectory() throws IOException {
        List<Path> dirsToMigrate = Arrays.asList(firstDir, secondDir);

        DefaultDirectoryMigrator defaultDirectoryMigrator = new DefaultDirectoryMigrator();

        List<Path> migrated = defaultDirectoryMigrator.migrate(dirsToMigrate, secondFirstDir);

        Assertions.assertEquals(2, migrated.size());
        List<Path> files = Files.list(secondFirstDir).collect(Collectors.toList());
        Assertions.assertEquals(4, files.size());
    }


    
}
