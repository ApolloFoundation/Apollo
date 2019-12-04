/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.HeightDbIdRequest;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;


class DexOrderDaoTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    DexOrderDao dexOrderDao;
    DexTestData td;

    @BeforeEach
    void setUp() {
        JdbiHandleFactory jdbiHandleFactory = new JdbiHandleFactory();
        jdbiHandleFactory.setJdbi(extension.getDatabaseManager().getJdbi());
        dexOrderDao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(jdbiHandleFactory, DexOrderDao.class);
        td = new DexTestData();
    }

    @Test
    void testGetOrdersByType() {
        List<DexOrder> orders = dexOrderDao.getOrders(DexOrderDBRequest.builder().type(0).build());

        assertEquals(List.of(td.ORDER_BPB_1, td.ORDER_BEA_1, td.ORDER_BEA_4), orders);
    }

    @Test
    void testGetOrdersByAccountAndDbId() {
        List<DexOrder> orders = dexOrderDao.getOrders(DexOrderDBRequest.builder().dbId(td.ORDER_SPA_2.getDbId()).accountId(td.ALICE).build());

        assertEquals(List.of(td.ORDER_SEA_3, td.ORDER_BEA_4), orders);
    }

    @Test
    void testGetLastClosedOrderBeforeHeight() {
        DexOrder order = dexOrderDao.getLastClosedOrderBeforeHeight(DexCurrency.ETH, 125);

        assertEquals(td.ORDER_BEA_8, order);
    }

    @Test
    void testGetClosedOrders() {
        List<DexOrder> orders = dexOrderDao.getClosedOrdersFromDbId(HeightDbIdRequest.builder()
                .coin(DexCurrency.ETH)
                .fromDbId(999)
                .limit(2)
                .toHeight(123)
                .build());

        assertEquals(List.of(td.ORDER_BEA_1), orders);
    }

    @Test
    void testGetClosedOrdersWithPagination() {
        List<DexOrder> orders = dexOrderDao.getClosedOrdersFromDbId(HeightDbIdRequest.builder()
                .coin(DexCurrency.ETH)
                .fromDbId(999)
                .limit(2)
                .toHeight(124)
                .build());

        assertEquals(List.of(td.ORDER_BEA_1, td.B), orders);
    }

}