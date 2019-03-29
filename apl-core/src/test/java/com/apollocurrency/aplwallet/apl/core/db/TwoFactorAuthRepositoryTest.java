/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.extension.RegisterExtension;


public class TwoFactorAuthRepositoryTest extends AbstractTwoFactorAuthRepositoryTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();
    public TwoFactorAuthRepositoryTest() {

        super(new TwoFactorAuthRepositoryImpl(dbExtension.getDatabaseManger().getDataSource()));
    }
}
