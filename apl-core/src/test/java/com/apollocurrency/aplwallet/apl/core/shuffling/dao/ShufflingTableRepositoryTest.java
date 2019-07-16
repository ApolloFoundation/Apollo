/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.shuffling.mapper.ShufflingMapper;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.extension.RegisterExtension;
@EnableWeld
public class ShufflingTableRepositoryTest extends ShufflingRepositoryTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/shuffling.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedTablesRegistry.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();

    @Override
    public ShufflingRepository repository() {
        ShufflingKeyFactory keyFactory = new ShufflingKeyFactory();
        return new ShufflingTable(keyFactory, new ShufflingMapper(keyFactory));
    }

    @Override
    void testDelete() {
        DbUtils.inTransaction(extension, (con)-> super.testDelete());
    }

    @Override
    void testInsert() {
        DbUtils.inTransaction(extension, (con) -> super.testInsert());
    }

    @Override
    void testInsertExisting() {
        DbUtils.inTransaction(extension, (con) -> super.testInsertExisting());
    }
}
