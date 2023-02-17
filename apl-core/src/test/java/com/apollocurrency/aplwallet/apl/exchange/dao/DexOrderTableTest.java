/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Tag("slow")
class DexOrderTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
    DexTestData td = new DexTestData();
    DexOrderTable table = new DexOrderTable(extension.getDatabaseManager(), mock(Event.class));

    @Test
    void getPendingOrdersWithoutContracts() {
        List<DexOrder> orders = table.getPendingOrdersWithoutContracts(Integer.MAX_VALUE);

        assertEquals(List.of(td.ORDER_BPB_2), orders);
    }
}