/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import java.io.IOException;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

public class DbIntegrationTest {
    private static DbManipulator manipulator;
    public DbIntegrationTest(Path path, String password, String user) throws IOException {
        manipulator = new DbManipulator(path, user, password);
            manipulator.init();
    }
    public DbIntegrationTest() {
        manipulator = new DbManipulator();
        manipulator.init();
    }
    @AfterAll
    public static void shutdown() throws Exception {
        manipulator.shutdown();
    }

    @BeforeEach
    public void setUp() throws Exception {
        manipulator.populate();
    }

    public BasicDb getDb() {
        return manipulator.getDb();
    }
}
