/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.dex.core.dao.UserErrorMessageDao;
import com.apollocurrency.aplwallet.apl.dex.core.model.UserErrorMessage;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("slow")
public class UserErrorMessageDaoTest extends DbContainerBaseTest {
    public final UserErrorMessage ERROR_1 = new UserErrorMessage(100L, "0x0398E119419E0D7792c53913d3f370f9202Ae137", "Invalid transaction", "deposit", "900", 1000);
    public final UserErrorMessage ERROR_2 = new UserErrorMessage(200L, "0x8e96e98b32c56115614B64704bA35feFE9e8f7bC", "Out of gas", "redeem", "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 1100);
    public final UserErrorMessage ERROR_3 = new UserErrorMessage(300L, "0x0398E119419E0D7792c53913d3f370f9202Ae137", "Double spending", "withdraw", "100", 1200);
    public final UserErrorMessage NEW_ERROR = new UserErrorMessage(301L, "0x0398E119419E0D7792c53913d3f370f9202Ae137", "No enough funds", "deposit", "100", 1300);

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    private UserErrorMessageDao dao;

    @BeforeEach
    void setUp() {
        dao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(DbUtils.createJdbiHandleFactory(extension.getDatabaseManager()), UserErrorMessageDao.class);
    }

    @Test
    void testGetAllWithPagination() {
        List<UserErrorMessage> all = dao.getAll(ERROR_2.getDbId() + 1, 1);

        assertEquals(List.of(ERROR_2), all);
    }

    @Test
    void testGetAll() {
        extension.cleanAndPopulateDb();

        List<UserErrorMessage> all = dao.getAll(Long.MAX_VALUE, 3);

        assertEquals(List.of(ERROR_3, ERROR_2, ERROR_1), all);
    }

    @Test
    void testAdd() {
        dao.add(NEW_ERROR);

        List<UserErrorMessage> all = dao.getAll(Long.MAX_VALUE, 3);
        assertEquals(List.of(NEW_ERROR, ERROR_3, ERROR_2), all);
    }

    @Test
    void testGetAllForUser() {
        List<UserErrorMessage> allByAddress = dao.getAllByAddress(ERROR_1.getAddress(), Long.MAX_VALUE, 3);

        assertEquals(List.of(ERROR_3, ERROR_1), allByAddress);
    }

    @Test
    void testGetAllForUserWithPagination() {
        List<UserErrorMessage> allByAddress = dao.getAllByAddress(ERROR_2.getAddress(), ERROR_2.getDbId(), 3);

        assertEquals(List.of(), allByAddress);
    }

    @Test
    void testDeleteAllByTimestamp() {
        dao.deleteByTimestamp(Long.MAX_VALUE);

        List<UserErrorMessage> all = dao.getAll(Long.MAX_VALUE, 100);

        assertEquals(List.of(), all);
    }

    @Test
    void testDeleteByTimestamp() {
        extension.cleanAndPopulateDb();

        dao.deleteByTimestamp(ERROR_2.getTimestamp() + 1);

        List<UserErrorMessage> all = dao.getAll(Long.MAX_VALUE, 100);

        assertEquals(List.of(ERROR_3), all);
    }

}
