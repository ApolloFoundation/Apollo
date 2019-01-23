/*
 * Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.migrator;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultDirectoryMigratorTest {
    private Path firstDirFile1;
    private Path firstDirFile2;
    private Path firstDir;
    private Path secondFile;
    private Path secondDir;
    private Path thirdFile;
    private Path thirdDir;
    private Path thirdNestedDir;
    private Path emptyDir;
    private Path targetFile;
    private Path targetDir;
    private byte[] firstFilePayload = "TestPayload-1".getBytes();
    private byte[] secondFilePayload = "TestPayload-2".getBytes();
    private byte[] thirdFilePayload = "TestPayload-3".getBytes();
    private byte[] targetFilePayload = "TargetPayload".getBytes();

    @BeforeEach
    public void init() throws IOException {

        firstDir = Files.createTempDirectory("d-1_");
        firstDirFile1 = Files.createTempFile(firstDir, "f-1_", "");
        Files.write(firstDirFile1, firstFilePayload);
        secondDir = Files.createTempDirectory("d-1_");
        secondFile = Files.createTempFile(secondDir, "f-2_", "");
        Files.write(secondFile, secondFilePayload);
        thirdDir = Files.createTempDirectory("d-3_");
        thirdNestedDir = Files.createTempDirectory(thirdDir, "d-3_1_");
        thirdFile = Files.createTempFile(thirdNestedDir, "f-3_", "");
        Files.write(thirdFile, thirdFilePayload);
        emptyDir = Files.createTempDirectory("empty");
        targetDir = Files.createTempDirectory("target");
        targetFile = Files.createTempFile(targetDir, "target", "");
        Files.write(targetFile, targetFilePayload);
        firstDirFile2 = Files.createFile(firstDir.resolve(targetFile.getFileName()));
        Files.write(firstDirFile2, firstFilePayload);
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(firstDir.toFile());
        FileUtils.deleteDirectory(secondDir.toFile());
        FileUtils.deleteDirectory(thirdDir.toFile());
        FileUtils.deleteDirectory(targetDir.toFile());
        FileUtils.deleteDirectory(emptyDir.toFile());
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
        Set<Path> expectedMigratedFilePaths = new HashSet<>(Arrays.asList(
                firstDirFile1TargetPath, firstDirFile2TargetPath, secondDirFileTargetPath));

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
    
}
