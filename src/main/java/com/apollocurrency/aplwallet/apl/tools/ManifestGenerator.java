/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.env.service.AplService_ServiceManagement;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestGenerator {

    public static void main(String[] args) {
        ManifestGenerator manifestGenerator = new ManifestGenerator();
        manifestGenerator.generate("./resource/apl.manifest.mf", Apl.class.getCanonicalName(), "./lib");
        String serviceClassName = AplService_ServiceManagement.class.getCanonicalName();
        serviceClassName = serviceClassName.substring(0, serviceClassName.length() - "_ServiceManagement".length());
        manifestGenerator.generate("./resource/aplservice.manifest.mf", serviceClassName, "./lib");
    }

    private void generate(String fileName, String className, String ... directories) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, className);
        StringBuilder classpath = new StringBuilder();

        for (String directory : directories) {
            DirListing dirListing = new DirListing();
            try {
                Files.walkFileTree(Paths.get(directory), EnumSet.noneOf(FileVisitOption.class), 2, dirListing);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            classpath.append(dirListing.getClasspath());
        }
        classpath.append("conf/");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classpath.toString());
        try {
            manifest.write(Files.newOutputStream(Paths.get(fileName), StandardOpenOption.CREATE));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class DirListing extends SimpleFileVisitor<Path> {

        private final StringBuilder classpath = new StringBuilder();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            Path dir = file.subpath(file.getNameCount() - 2, file.getNameCount() - 1);
            classpath.append(dir).append('/').append(file.getFileName()).append(' ');
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            return FileVisitResult.CONTINUE;
        }

        public StringBuilder getClasspath() {
            return classpath;
        }
    }

    private static class JavaFilesVisitor extends SimpleFileVisitor<Path> {

        private List<Path> javaFiles = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (file.getFileName().toString().endsWith(".java")) {
                javaFiles.add(file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            return FileVisitResult.CONTINUE;
        }

        public List<Path> getJavaFiles() {
            return javaFiles;
        }
    }
}
