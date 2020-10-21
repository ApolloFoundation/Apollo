/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.db.AbstractTwoFactorAuthRepositoryTest;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Tag("slow")
public class TwoFactorAuthRepositoryTest extends AbstractTwoFactorAuthRepositoryTest {

    @RegisterExtension
    DbExtension dbExtension = new DbExtension(DbTestData.getDbFileProperties(mariaDBContainer));

    @BeforeEach
    public void setUp() {
        repository = new TwoFactorAuthRepositoryImpl(dbExtension.getDatabaseManager().getDataSource());
    }
}
