/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.db.BasicDb;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import util.DbManipulator;

public class DbIntegrationTest {
    private static final DbManipulator manipulator = new DbManipulator();
    protected static final BasicDb db = manipulator.getDb();
    @BeforeClass
    public static void init() throws SQLException {
        manipulator.init();
    }
    @AfterClass
    public static void shutdown() throws Exception {
        manipulator.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        manipulator.populate();
    }
}
