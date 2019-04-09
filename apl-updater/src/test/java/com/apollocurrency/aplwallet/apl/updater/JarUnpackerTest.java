/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.updater.util.JarGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class JarUnpackerTest {
    private Path tempDirectory;
    private Path tempJar;
    private Path unpackedFile;

    @BeforeEach
    public void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("unpack");
        tempJar = Files.createTempFile(tempDirectory, "test", ".jar");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.deleteIfExists(tempJar);
        Files.deleteIfExists(tempDirectory);
        if (unpackedFile != null) {
            deleteDir(unpackedFile, path -> true);
        }
    }

    @Test
    public void testUnpack() throws IOException {
            try (
                    OutputStream outputStream = Files.newOutputStream(tempJar);
                    JarGenerator generator = new JarGenerator(outputStream)) {
            generator.generate();
            }
            Unpacker unpacker = new JarUnpacker("");
            unpackedFile = unpacker.unpack(tempJar);
            try (JarFile jarFile = new JarFile(tempJar.toFile())) {
                Map<String, Long> files = new HashMap<>();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    files.put(jarEntry.getName(), jarEntry.getSize());
                }
                Path finalUnpackedFile = unpackedFile;
                Files.walkFileTree(unpackedFile, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        Path unpackedFile = finalUnpackedFile.relativize(file);
                        assertTrue(files.containsKey(unpackedFile.toString().replaceAll("\\\\", "/")));
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
    }
    public static void deleteDir(Path dir, Predicate<Path> deleteFilter) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (deleteFilter.test(file)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (deleteFilter.test(dir)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
}
