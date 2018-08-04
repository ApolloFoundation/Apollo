/*
 * Copyright © 2018 Apollo Foundation
 */

package apl.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Unpacker {

    private static class UnpackerHolder {
        private static final Unpacker INSTANCE = new Unpacker();
    }

    private Unpacker() {}

    public static Unpacker getInstance() {
        return UnpackerHolder.INSTANCE;
    }
    public Path unpack(Path jarFilePath) throws IOException {
        Path destDirPath = Files.createTempDirectory("apollo-unpacked");
        JarFile jar = new JarFile(jarFilePath.toString());
        Enumeration enumEntries = jar.entries();
        while (enumEntries.hasMoreElements()) {
            JarEntry file = (JarEntry) enumEntries.nextElement();
            Path f = destDirPath.resolve(file.getName());
            if (file.isDirectory()) {
                Files.createDirectory(f);
                continue;
            }
            Files.copy(jar.getInputStream(file), f);
        }
        jar.close();
        return destDirPath;
    }
}
