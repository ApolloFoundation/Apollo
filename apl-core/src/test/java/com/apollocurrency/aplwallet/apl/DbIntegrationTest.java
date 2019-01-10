/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.BasicDb;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class DbIntegrationTest {
    private static final DbManipulator manipulator = new DbManipulator();
    protected static final BasicDb db = manipulator.getDb();

    @BeforeAll
    public static void init() throws SQLException {
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
}
