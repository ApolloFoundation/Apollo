/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTransaction;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Testcontainers
@Tag("slow")
class DexTransactionDaoTest {
    @Container
    public static final GenericContainer mariaDBContainer = new MariaDBContainer("mariadb:10.4")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withExposedPorts(3306)
        .withLogConsumer(new Slf4jLogConsumer(log));

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);
    DexTransactionDao dao;
    DexTestData td;

    @BeforeEach
    void setUp() {
        JdbiHandleFactory jdbiHandleFactory = new JdbiHandleFactory();
        jdbiHandleFactory.setJdbi(extension.getDatabaseManager().getJdbi());
        dao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(jdbiHandleFactory, DexTransactionDao.class);
        td = new DexTestData();
    }


    @Test
    void testGet() {
        DexTransaction dexTransaction = dao.get(td.TX_1.getParams(), td.TX_1.getAccount(), td.TX_1.getOperation());

        assertEquals(td.TX_1, dexTransaction);
    }

    @Test
    void testAdd() {
        td.TX_2.setOperation(DexTransaction.Op.REFUND);
        td.TX_2.setDbId(td.TX_3.getDbId() + 1);
        dao.add(td.TX_2);

        assertEquals(td.TX_2, dao.get(td.TX_2.getParams(), td.TX_2.getAccount(), td.TX_2.getOperation()));
        assertEquals(4, dao.getAll(0, 100).size());
    }

    @Test
    void testUpdate() {
        td.TX_1.setTimestamp(1000);
        dao.update(td.TX_1);

        assertEquals(td.TX_1, dao.get(td.TX_1.getDbId()));
        assertEquals(3, dao.getAll(0, 100).size());
    }

    @Test
    void testDeleteAllBeforeTimestamp() {
        dao.deleteAllBeforeTimestamp(td.TX_2.getTimestamp());

        List<DexTransaction> all = dao.getAll(0, 100);
        assertEquals(List.of(td.TX_2, td.TX_3), all);
    }

    @Test
    void testDelete() {
        dao.delete(td.TX_3.getDbId());

        List<DexTransaction> all = dao.getAll(0, 100);
        assertEquals(List.of(td.TX_1, td.TX_2), all);
    }

    @Test
    void testGetAll() {
        List<DexTransaction> all = dao.getAll(0, 100);

        assertEquals(List.of(td.TX_1, td.TX_2, td.TX_3), all);
    }

    @Test
    void testGetAllWithPagination() {
        List<DexTransaction> all = dao.getAll(td.TX_1.getDbId(), 1);

        assertEquals(List.of(td.TX_2), all);
    }

}