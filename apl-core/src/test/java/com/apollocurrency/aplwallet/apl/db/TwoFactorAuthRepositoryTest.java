/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthRepositoryImpl;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.AbstractTwoFactorAuthRepositoryTest;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;


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

    @AfterAll
    public static void shutdown() throws Exception {
        manipulator.shutdown();
    }

    @BeforeEach
    public void setUp() throws Exception {
        manipulator.populate();
    }


}
