/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j
@Tag("slow")
public class DexContractTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    DexContractTable table = new DexContractTable(extension.getDatabaseManager(), mock(Event.class));
    DexTestData td;

    @BeforeEach
    void setUp() {
        td = new DexTestData();
    }

    @Test
    void testInsert() {
        DbUtils.inTransaction(extension, (con) -> {
            table.insert(td.NEW_EXCHANGE_CONTRACT_16);
        });
        ExchangeContract result = table.getById(td.NEW_EXCHANGE_CONTRACT_16.getId());
        assertNotNull(result);
        assertEquals(td.NEW_EXCHANGE_CONTRACT_16.getId(), result.getId());
    }

    @Test
    void testGetAll() {
        DbIterator<ExchangeContract> iterator = table.getAll(0, 10);
        List<ExchangeContract> result = CollectionUtil.toList(iterator);
        assertEquals(10, result.size());
    }

    @Test
    void testGetByCounterOrder() {
        List<ExchangeContract> allByCounterOrder = table.getAllByCounterOrder(-6968465014361285240L);

        assertEquals(List.of(td.EXCHANGE_CONTRACT_11, td.EXCHANGE_CONTRACT_12, td.EXCHANGE_CONTRACT_13,
            td.EXCHANGE_CONTRACT_14, td.EXCHANGE_CONTRACT_15), allByCounterOrder);
    }

}
