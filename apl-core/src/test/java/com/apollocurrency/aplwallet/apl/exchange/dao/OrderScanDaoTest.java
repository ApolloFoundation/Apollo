/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderScan;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.UndeclaredThrowableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@Testcontainers
@Tag("slow")
class OrderScanDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/dex-order-scan-data.sql", null);
    private OrderScanDao dao;

    @BeforeEach
    void setUp() {
        dao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(
            extension.getDatabaseManager().getJdbiHandleFactory(), OrderScanDao.class);
    }

    @Test
    void testAdd() {
        OrderScan expected = new OrderScan(DexCurrency.APL, 20);
        dao.add(expected);
        OrderScan orderScan = dao.get(DexCurrency.APL);

        assertEquals(expected, orderScan);
    }

    @Test
    void testUpdate() {
        OrderScan expected = new OrderScan(DexCurrency.PAX, 1000);
        dao.update(expected);
        OrderScan actual = dao.get(DexCurrency.PAX);

        assertEquals(expected, actual);
    }

    @Test
    void testGet() {
        OrderScan orderScan = dao.get(DexCurrency.PAX);

        assertEquals(new OrderScan(DexCurrency.PAX, 200), orderScan);
    }

    @Test
    void testAddExistingCoin() {
        assertThrows(UndeclaredThrowableException.class, () -> dao.add(new OrderScan(DexCurrency.ETH, 1)));
    }
}