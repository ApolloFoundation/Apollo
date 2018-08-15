/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.fs;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class LocalStorage implements Storage {

    private final String baseDir;

    public LocalStorage(String baseDir) {
        this.baseDir = baseDir;
    }

    private void createDirsForFile(File file) {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }
    }

    @Override
    public boolean put(String key, byte[] data) {
        File file = new File(baseDir, key);
        createDirsForFile(file);
        try {
            FileUtils.writeByteArrayToFile(file, data);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public byte[] get(String key) {
        File file = new File(baseDir, key);
        try {
            return FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void delete(String key) {
        File file = new File(baseDir, key);
        file.delete();
    }

}
