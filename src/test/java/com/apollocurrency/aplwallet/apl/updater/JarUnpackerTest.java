/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.updater.util.util.JarGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import util.TestUtil;

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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUnpackerTest {
    private Path tempDirectory;
    private Path tempJar;
    private Path unpackedFile;
    @Before
    public void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("unpack");
        tempJar = Files.createTempFile(tempDirectory, "test", ".jar");
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(tempJar);
        Files.deleteIfExists(tempDirectory);
        if (unpackedFile != null) {
            TestUtil.deleteDir(unpackedFile, path -> true);
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
                        Assert.assertTrue(files.containsKey(unpackedFile.toString().replaceAll("\\\\", "/")));
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
    }

}
