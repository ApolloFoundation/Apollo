/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.dex.core.model.DBSortOrder;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexOrderSortBy;
import com.apollocurrency.aplwallet.apl.dex.core.model.HeightDbIdRequest;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderDbIdPaginationDbRequest;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j

@Tag("slow")
class DexOrderDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
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
        List<DexOrder> orders = dexOrderDao.getOrders(
            DexOrderDBRequest.builder().type(OrderType.SELL.ordinal()).limit(10).offset(0).build(),
            DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);

        assertEquals(List.of(td.ORDER_SPA_2, td.ORDER_SEA_7, td.ORDER_SEA_3), orders); // sorted by pair rate desc
    }

    @Test
    void testGetOrdersByAccountAndDbId() {
        List<DexOrder> orders = dexOrderDao.getOrders(DexOrderDBRequest.builder()
                .dbId(td.ORDER_SPA_2.getDbId())
                .accountId(td.BOB)
                .pairCur(DexCurrency.PAX.ordinal())
                .limit(10).offset(0)
                .build(),
            DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);

        assertEquals(List.of(td.ORDER_BPB_1, td.ORDER_BPB_2), orders);
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
            .toHeight(125)
            .build());

        assertEquals(List.of(td.ORDER_BEA_1, td.ORDER_BEA_8), orders);
    }

    @Test
    void testGetLastOrderBeforeTimestamp() {
        DexOrder order = dexOrderDao.getLastClosedOrderBeforeTimestamp(DexCurrency.PAX, td.ORDER_BEA_6.getFinishTime());

        assertEquals(td.ORDER_BPA_5, order);
    }

    @Test
    void testGetNoClosedBuyOrdersBetweenTimestamps() {
        List<DexOrder> orders = dexOrderDao.getOrdersFromDbIdBetweenTimestamps(OrderDbIdPaginationDbRequest.builder()
            .limit(3)
            .coin(DexCurrency.ETH)
            .fromDbId(0)
            .fromTime(11_000)
            .toTime(17_000)
            .build());

        assertEquals(List.of(), orders);

    }

    @Test
    void testGetClosedBuyOrdersBetweenTimestamps() {
        List<DexOrder> orders = dexOrderDao.getOrdersFromDbIdBetweenTimestamps(OrderDbIdPaginationDbRequest.builder()
            .limit(3)
            .coin(DexCurrency.ETH)
            .fromDbId(1030)
            .fromTime(6_001)
            .toTime(19_001)
            .build());

        assertEquals(List.of(td.ORDER_BEA_8), orders);

    }

}