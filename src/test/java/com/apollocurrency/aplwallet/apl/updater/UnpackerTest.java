/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class UnpackerTest {
    @Test
    public void testUnpack() throws IOException {
        Path tempDirectory = Files.createTempDirectory("unpack");
        Path tempJar = Files.createTempFile(tempDirectory, "test", ".jar");
        Path unpackedFile = null;
        try (OutputStream outputStream = Files.newOutputStream(tempJar)) {
            JarGenerator generator = new JarGenerator(outputStream);
            generator.generate();
            generator.close();
            outputStream.close();
            unpackedFile = Unpacker.getInstance().unpack(tempJar);
            try (JarFile jarFile = new JarFile(tempJar.toFile())) {
                Map<String, Long> files = new HashMap<>();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    files.put(jarEntry.getName(), jarEntry.getSize());
                }
                jarFile.close();
                Path finalUnpackedFile = unpackedFile;
                Files.walkFileTree(unpackedFile, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        Path unpackedFile = finalUnpackedFile.relativize(file);
                        Assert.assertTrue(files.containsKey(unpackedFile.toString().replaceAll("\\\\", "/")));
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        finally {
            Files.deleteIfExists(tempJar);
            Files.deleteIfExists(tempDirectory);
            if (unpackedFile != null) {
                Files.walkFileTree(unpackedFile, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

}
