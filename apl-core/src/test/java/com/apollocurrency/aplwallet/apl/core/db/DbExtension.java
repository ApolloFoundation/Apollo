/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DbExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback, BeforeAllCallback {
    private DbManipulator manipulator;
    private boolean staticInit = false;

    public DbExtension(Path path) {
        manipulator = new DbManipulator(path);
    }

    public DbExtension() {
        manipulator = new DbManipulator();
    }

    public DatabaseManager getDatabaseManger() {
        return manipulator.getDatabaseManager();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (!staticInit) {
            shutdownDbAndDelete();
        }
    }

    private void shutdownDbAndDelete() throws IOException {
        manipulator.shutdown();
        Path tempDbFile = manipulator.getTempDbFile();
        if (tempDbFile != null) {
            Files.delete(Paths.get(tempDbFile.toAbsolutePath().toString() + ".h2.db"));
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (!staticInit) {manipulator.init();}

        manipulator.populate();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        shutdownDbAndDelete();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        staticInit = true;
        manipulator.init();
    }
}
