/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Unpacker {

    private Unpacker() {}

    public static Unpacker getInstance() {
        return UnpackerHolder.INSTANCE;
    }

    public static void main(String[] args) throws IOException {
        Path unpack = getInstance().unpack(Paths.get("C:/users/zandr/downloads/Apollo-1.0.10.jar"));

        System.out.println(unpack);
    }

    public Path unpack(Path jarFilePath) throws IOException {
        Path destDirPath = Files.createTempDirectory("apollo-unpacked");
        try (JarFile jar = new JarFile(jarFilePath.toFile())) {

        // fist get all directories,
        // then make those directory on the destination Path
        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
            JarEntry entry = enums.nextElement();

            String fileName = destDirPath.toAbsolutePath() + File.separator + entry.getName();
            File f = new File(fileName);

            if (fileName.endsWith("/") || entry.isDirectory()) {
                f.mkdirs();
            }

        }
        //now create all files
        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
            JarEntry entry = enums.nextElement();

            String fileName = destDirPath.toAbsolutePath().toString() + File.separator + entry.getName();
            File f = new File(fileName);
            if (!fileName.endsWith("/") && !entry.isDirectory()) {
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
                try (InputStream is = jar.getInputStream(entry)) {
                    copyPath(is, f);
                }
            }
        }
            return destDirPath;
        }
    }


        private static class UnpackerHolder {
            private static final Unpacker INSTANCE = new Unpacker();
        }

    private void copy(InputStream is, File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            byte[] buff = new byte[4096];
            int c;
            while ((c = is.read(buff)) != -1) {
                fos.write(buff, 0, c);
            }
        }
    }

    private void copyPath(InputStream is, File f) throws IOException {
        Files.copy(is, f.toPath());
    }
}
