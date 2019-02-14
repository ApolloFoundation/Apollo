/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import javax.sql.DataSource;

import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Path;

public class DbTest {
    private static DbManipulator manipulator;
    public DbTest(Path path, String password, String user) throws IOException {
        manipulator = new DbManipulator(path, user, password);
    }
    public DbTest() {
        manipulator = new DbManipulator();
    }

    @BeforeEach
    public void setUp() throws Exception {
        manipulator.init();
        manipulator.populate();
    }

    @AfterEach
    public void tearDown() throws Exception {
        manipulator.shutdown();
    }

    public DataSource getDataSource() {
        return manipulator.getDataSource();
    }
}
