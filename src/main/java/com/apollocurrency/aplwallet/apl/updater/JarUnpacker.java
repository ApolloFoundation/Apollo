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
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUnpacker implements Unpacker {
    private String unpackedDirectoryPrefix;

    public JarUnpacker(String unpackedDirectoryPrefix) {
        this.unpackedDirectoryPrefix = unpackedDirectoryPrefix;
    }

    @Override
    public Path unpack(Path jarFilePath) throws IOException {
        Path destDirPath = Files.createTempDirectory(unpackedDirectoryPrefix);
        try (
            JarFile jar = new JarFile(jarFilePath.toFile())) {


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
                    //create all parent dirs if not exist
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
