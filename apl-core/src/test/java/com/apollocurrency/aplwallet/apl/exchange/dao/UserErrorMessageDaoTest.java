/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.eth.dao.UserErrorMessageDao;
import com.apollocurrency.aplwallet.apl.exchange.model.UserErrorMessage;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.WeldUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("slow")
@EnableWeld
@Disabled // TODO move integration tests
public class UserErrorMessageDaoTest {
    public final UserErrorMessage ERROR_1 = new UserErrorMessage(100L, "0x0398E119419E0D7792c53913d3f370f9202Ae137", "Invalid transaction", "deposit", "900", 1000);
    public final UserErrorMessage ERROR_2 = new UserErrorMessage(200L, "0x8e96e98b32c56115614B64704bA35feFE9e8f7bC", "Out of gas", "redeem", "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 1100);
    public final UserErrorMessage ERROR_3 = new UserErrorMessage(300L, "0x0398E119419E0D7792c53913d3f370f9202Ae137", "Double spending", "withdraw", "100", 1200);
    public final UserErrorMessage NEW_ERROR = new UserErrorMessage(301L, "0x0398E119419E0D7792c53913d3f370f9202Ae137", "No enough funds", "deposit", "100", 1300);


    @RegisterExtension
    DbExtension extension = new DbExtension();

    @WeldSetup
    WeldInitiator weld = WeldUtils.from(List.of(UserErrorMessageDao.class, DaoConfig.class), List.of())
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .build();

    @Inject
    UserErrorMessageDao dao;

    @Test
    void testGetAllWithPagination() {
        List<UserErrorMessage> all = dao.getAll(ERROR_2.getDbId() + 1, 1);

        assertEquals(List.of(ERROR_2), all);
    }

    @Test
    void testGetAll() {
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
        dao.deleteByTimestamp(ERROR_2.getTimestamp() + 1);

        List<UserErrorMessage> all = dao.getAll(Long.MAX_VALUE, 100);

        assertEquals(List.of(ERROR_3), all);
    }

}
