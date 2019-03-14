/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;

public class TemporaryFolderExtension implements BeforeEachCallback, AfterEachCallback {

    private final File parentFolder;
    private File folder;

    public TemporaryFolderExtension() {
        this(null);
    }

    public TemporaryFolderExtension(File parentFolder) {
        this.parentFolder = parentFolder;
    }

    public void delete() {
        if (folder != null) {
            recursiveDelete(folder);
        }
    }
    @Override
    public void afterEach(ExtensionContext extensionContext) {
        delete();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws IOException {
        create();
    }

    public void create() throws IOException {
        folder = File.createTempFile("junit", "", parentFolder);
        folder.delete();
        folder.mkdirs();
    }

    public File newFile(String fileName) throws IOException {
        File file = new File(getRoot(), fileName);
        if (!file.createNewFile()) {
            throw new IOException("a file with the name \'" + fileName + "\' already exists in the test folder");
        }
        return file;
    }

    public File newFolder(String folderName) {
        File file = getRoot();
        file = new File(file, folderName);
        file.mkdirs();
        return file;
    }
    public File newFolder() throws IOException {
        File file = getRoot();
        file =  File.createTempFile("junit", "", file);
        file.delete();
        file.mkdirs();
        return file;
    }

    private void recursiveDelete(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                recursiveDelete(each);
            }
        }
        file.delete();
    }

    public File getRoot() {
        if (folder == null) {
            throw new IllegalStateException("the temporary folder has not yet been created");
        }
        return folder;
    }

}
