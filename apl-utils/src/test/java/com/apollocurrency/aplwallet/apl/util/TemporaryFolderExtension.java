/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TemporaryFolderExtension implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {
    private final File parentFolder;
    private File folder;
    private File rootFolder;

    public TemporaryFolderExtension() {
        this(null);
    }

    public TemporaryFolderExtension(File parentFolder) {
        this.parentFolder = parentFolder;
    }

    public void delete(File file) {
        Objects.requireNonNull(file);
        recursiveDelete(file);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        delete(folder);
        folder = null;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws IOException {
        folder = create(rootFolder == null ? parentFolder : rootFolder);
    }

    File create(File parentFolder) throws IOException {
        File f = File.createTempFile("junit", "", parentFolder);
        f.delete();
        f.mkdirs();
        return f;
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
        file = File.createTempFile("junit", "", file);
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
        if (folder == null && rootFolder == null) {
            throw new IllegalStateException("the temporary folder has not yet been created");
        }
        return folder == null ? rootFolder : folder;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        rootFolder = create(parentFolder);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        delete(rootFolder);
    }
}
