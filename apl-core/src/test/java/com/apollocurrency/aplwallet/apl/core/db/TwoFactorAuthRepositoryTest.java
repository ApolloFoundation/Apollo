/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;


public class TwoFactorAuthRepositoryTest extends AbstractTwoFactorAuthRepositoryTest {
    private static DbManipulator manipulator = new DbManipulator();

    static {
            manipulator.init();
    }

    public TwoFactorAuthRepositoryTest() {

        super(new TwoFactorAuthRepositoryImpl(manipulator.getDataSource()));
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
