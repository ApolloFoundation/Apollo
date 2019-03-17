/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sql.DataSource;

public class DbExtension implements BeforeEachCallback, AfterEachCallback {
    private DbManipulator manipulator;

    public DbExtension(Path path, String password, String user) {
        manipulator = new DbManipulator(path, user, password);
    }

    public DbExtension() {
        manipulator = new DbManipulator();
    }

    public DataSource getDataSource() {
        return manipulator.getDataSource();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        manipulator.shutdown();
        Path tempDbFile = manipulator.getTempDbFile();
        if (tempDbFile != null) {
            Files.delete(Paths.get(tempDbFile.toAbsolutePath().toString() + ".h2.db"));
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        manipulator.init();
        manipulator.populate();
    }
}
