package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.LinkKey;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
@EnableWeld
class ShufflingDataTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/shuffling.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedTablesRegistry.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class),
                    MockBean.of(mock(Blockchain.class), Blockchain.class),
                    MockBean.of(mock(PropertiesHolder.class), PropertiesHolder.class),
                    MockBean.of(mock(EpochTime.class), EpochTime.class),
                    MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class)
            )
            .build();
    private ShufflingDataTable table;
    private ShufflingTestData std;
    @BeforeEach
    void setUp() {
        table = new ShufflingDataTable();
        std = new ShufflingTestData();
    }

    @Test
    void testGet() {
        ShufflingData data = table.get(std.DATA_5_A.getShufflingId(), std.DATA_5_A.getAccountId());
        assertEquals(std.DATA_5_A, data);
    }

    @Test
    void testInsert() {
        DbUtils.inTransaction(extension, (con) -> table.insert(std.NEW_DATA));

        ShufflingData shufflingData = table.get(std.NEW_DATA.getShufflingId(), std.NEW_DATA.getAccountId());

        assertEquals(std.NEW_DATA, shufflingData);

        std.NEW_DATA.setDbKey(new LinkKey(std.NEW_DATA.getShufflingId(), std.NEW_DATA.getAccountId()));
        std.NEW_DATA.setHeight(std.NEW_DATA.getHeight() + 1);
        std.NEW_DATA.setDbId(std.NEW_DATA.getDbId() + 1);

        DbUtils.inTransaction(extension, (con) -> table.insert(std.NEW_DATA));

        shufflingData = table.get(std.NEW_DATA.getShufflingId(), std.NEW_DATA.getAccountId());

        assertEquals(std.NEW_DATA, shufflingData);
    }

}
