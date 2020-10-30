/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UserErrorMessageDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.data.UserErrorMessageTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.UserErrorMessage;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.WeldUtils;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j

@Tag("slow")
@EnableWeld
public class UserErrorMessageDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    @WeldSetup
    WeldInitiator weld = WeldUtils.from(List.of(UserErrorMessageDao.class, DaoConfig.class), List.of())
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .build();

    @Inject
    UserErrorMessageDao dao;

    private UserErrorMessageTestData td = new UserErrorMessageTestData();

    @Test
    void testGetAllWithPagination() {
        List<UserErrorMessage> all = dao.getAll(td.ERROR_2.getDbId() + 1, 1);

        assertEquals(List.of(td.ERROR_2), all);
    }

    @Test
    void testGetAll() {
        List<UserErrorMessage> all = dao.getAll(Long.MAX_VALUE, 3);

        assertEquals(List.of(td.ERROR_3, td.ERROR_2, td.ERROR_1), all);
    }

    @Test
    void testAdd() {
        dao.add(td.NEW_ERROR);

        List<UserErrorMessage> all = dao.getAll(Long.MAX_VALUE, 3);
        assertEquals(List.of(td.NEW_ERROR, td.ERROR_3, td.ERROR_2), all);
    }

    @Test
    void testGetAllForUser() {
        List<UserErrorMessage> allByAddress = dao.getAllByAddress(td.ERROR_1.getAddress(), Long.MAX_VALUE, 3);

        assertEquals(List.of(td.ERROR_3, td.ERROR_1), allByAddress);
    }

    @Test
    void testGetAllForUserWithPagination() {
        List<UserErrorMessage> allByAddress = dao.getAllByAddress(td.ERROR_2.getAddress(), td.ERROR_2.getDbId(), 3);

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
        dao.deleteByTimestamp(td.ERROR_2.getTimestamp() + 1);

        List<UserErrorMessage> all = dao.getAll(Long.MAX_VALUE, 100);

        assertEquals(List.of(td.ERROR_3), all);
    }

}
