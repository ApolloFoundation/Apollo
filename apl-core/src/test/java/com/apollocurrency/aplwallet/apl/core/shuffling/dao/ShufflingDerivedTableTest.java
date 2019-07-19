/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.shuffling.mapper.ShufflingMapper;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

@EnableWeld
public class ShufflingDerivedTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/shuffling.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            ShufflingTable.class,
            ShufflingMapper.class,
            ShufflingKeyFactory.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();

    ShufflingTestData std;

    @Inject
    ShufflingTable shufflingTable;

    @BeforeEach
    public void setUp() {
        std = new ShufflingTestData();
    }
}
