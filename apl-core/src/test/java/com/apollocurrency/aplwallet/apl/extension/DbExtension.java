/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.extension;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class DbExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback, BeforeAllCallback {
    private static final Logger log = LoggerFactory.getLogger(DbExtension.class);
    private DbManipulator manipulator;
    private boolean staticInit = false;

    public DbExtension(Path path) {
        manipulator = new DbManipulator(path);
    }

    public DbExtension(DbProperties dbProperties) {
        this(dbProperties, null);
    }

    public DbExtension(DbProperties dbProperties, PropertiesHolder propertiesHolder) {
        manipulator = new DbManipulator(dbProperties, propertiesHolder);
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
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (!staticInit) {manipulator.init();}
        manipulator.populate();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        shutdownDbAndDelete();
        staticInit = false;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        staticInit = true;
        manipulator.init();
    }
}
