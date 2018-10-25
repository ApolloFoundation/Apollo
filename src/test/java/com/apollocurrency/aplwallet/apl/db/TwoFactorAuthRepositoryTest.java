/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.AbstractTwoFactorAuthRepositoryTest;
import org.junit.AfterClass;
import org.junit.Before;
import util.DbManipulator;


public class TwoFactorAuthRepositoryTest extends AbstractTwoFactorAuthRepositoryTest {
    private static final DbManipulator manipulator = new DbManipulator();
static {
    try {
        manipulator.init();
    }
    catch (SQLException e) {
        e.printStackTrace();
    }
}
    public TwoFactorAuthRepositoryTest() {

        super(new TwoFactorAuthRepositoryImpl(manipulator.getDb()));
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
