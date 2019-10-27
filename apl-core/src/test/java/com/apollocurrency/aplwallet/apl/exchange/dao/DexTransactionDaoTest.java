package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

class DexTransactionDaoTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
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
    void get() {
    }

    @Test
    void add() {
    }

    @Test
    void update() {
    }

    @Test
    void deleteAllBeforeTimestamp() {
    }

    @Test
    void delete() {
    }

    @Test
    void getAll() {
    }
}