package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.DexOperationTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOperation;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DexOperationDaoTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/dex-operation-data.sql", null);
    private DexOperationDao dao;

    private DexOperationTestData td;

    @BeforeEach
    void setUp() {
        dao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(
            extension.getDatabaseManager().getJdbiHandleFactory(), DexOperationDao.class);
        td = new DexOperationTestData();
    }

    @Test
    void testGet() {
        DexOperation op = dao.getBy(td.OP_1.getAccount(), td.OP_1.getStage(), td.OP_1.getEid());

        assertEquals(td.OP_1, op);
    }

    @Test
    void testUpdate() {
        td.OP_2.setAccount("Unknown account");
        int updated = dao.updateByDbId(td.OP_2);

        assertEquals(1, updated);
        assertEquals(td.OP_2, dao.getBy("Unknown account", td.OP_2.getStage(), td.OP_2.getEid()));
    }

    @Test
    void testAdd() {
        DexOperation newOp = new DexOperation(null, "New acc", DexOperation.Stage.APL_CONTRACT_S2, "100", null, null, false, new Timestamp(System.currentTimeMillis()));
        long dbId = dao.add(newOp);

        assertEquals(1005, dbId);
        DexOperation savedOp = dao.getBy("New acc", DexOperation.Stage.APL_CONTRACT_S2, "100");
        newOp.setDbId(1005L);
        assertEquals(newOp, savedOp);
    }

    @Test
    void testUniqueConstraint() {
        DexOperation newOp = new DexOperation(null, td.OP_4.getAccount(), td.OP_4.getStage(), td.OP_4.getEid(), null, null, false, new Timestamp(System.currentTimeMillis()));

        assertThrows(UndeclaredThrowableException.class, () -> dao.add(newOp));
    }

    @Test
    void testDelete() {
        int deleted = dao.deleteAfterTimestamp(td.OP_2.getTs());

        assertEquals(2, deleted);
        List<DexOperation> all = dao.getAll(1001, 3);
        assertEquals(List.of(td.OP_2, td.OP_4), all);

    }

    @Test
    void testGetAll() {
        List<DexOperation> all = dao.getAll(1001, 2);

        assertEquals(List.of(td.OP_2, td.OP_3), all);
    }


}
