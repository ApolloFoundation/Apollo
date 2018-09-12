/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.updater.util.JarGenerator;
import org.junit.Assert;
import org.junit.Test;
import util.TestUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUnpackerTest {
    @Test
    public void testUnpack() throws IOException {
        Path tempDirectory = Files.createTempDirectory("unpack");
        Path tempJar = Files.createTempFile(tempDirectory, "test", ".jar");
        Path unpackedFile = null;
        try {
        try (JarGenerator generator = new JarGenerator(Files.newOutputStream(tempJar))) {
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
                        Assert.assertTrue(files.containsKey(unpackedFile.toString().replaceAll("\\\\", "/")));
                        return FileVisitResult.CONTINUE;
                    }
                });
                jarFile.close();
            }
        }
        finally {
            Files.deleteIfExists(tempJar);
            Files.deleteIfExists(tempDirectory);
            if (unpackedFile != null) {
                TestUtil.deleteDir(unpackedFile, path -> true);
            }
        }
    }

}
