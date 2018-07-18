/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.updater;

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
