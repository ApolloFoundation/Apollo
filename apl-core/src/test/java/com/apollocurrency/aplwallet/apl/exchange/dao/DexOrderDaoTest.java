package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


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

}