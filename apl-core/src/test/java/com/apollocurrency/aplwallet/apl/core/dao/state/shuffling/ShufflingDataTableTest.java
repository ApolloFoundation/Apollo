/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DBContainerRootTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKey;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
@Tag("slow")
class ShufflingDataTableTest extends DBContainerRootTest {
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/shuffling.sql", null);

    private ShufflingDataTable table;
    private ShufflingTestData std;
    @BeforeEach
    void setUp() {
        table = new ShufflingDataTable(extension.getDatabaseManager(),mock(BlockchainConfig.class), mock(PropertiesHolder.class), mock(Event.class));
        std = new ShufflingTestData();
    }

    @Test
    void testGet() {
        byte[][] data = table.getData(std.DATA_5_A.getShufflingId(), std.DATA_5_A.getAccountId());
        assertArrayEquals(std.DATA_5_A.getData(), data);
    }

    @Test
    void testInsert() {
        DbUtils.inTransaction(extension, (con) -> table.insert(std.NEW_DATA));

        byte[][] shufflingData = table.getData(std.NEW_DATA.getShufflingId(), std.NEW_DATA.getAccountId());

        assertArrayEquals(std.NEW_DATA.getData(), shufflingData);

        std.NEW_DATA.setDbKey(new LinkKey(std.NEW_DATA.getShufflingId(), std.NEW_DATA.getAccountId()));
        std.NEW_DATA.setHeight(std.NEW_DATA.getHeight() + 1);
        std.NEW_DATA.setDbId(std.NEW_DATA.getDbId() + 1);

        DbUtils.inTransaction(extension, (con) -> table.insert(std.NEW_DATA));

        shufflingData = table.getData(std.NEW_DATA.getShufflingId(), std.NEW_DATA.getAccountId());

        assertArrayEquals(std.NEW_DATA.getData(), shufflingData);
    }

}
