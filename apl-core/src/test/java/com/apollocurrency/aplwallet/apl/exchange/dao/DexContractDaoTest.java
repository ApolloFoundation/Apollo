/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
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
public class DexContractDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);

    @WeldSetup
    WeldInitiator weld = WeldUtils.from(List.of(DexContractDao.class, DaoConfig.class), List.of())
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .build();

    @Inject
    DexContractDao dao;
    DexTestData td = new DexTestData();

    @Test
    void testGetContractsForAccount() {
        List<ExchangeContract> contract = dao.getAllForAccountOrder(td.EXCHANGE_CONTRACT_2.getSender(), td.EXCHANGE_CONTRACT_2.getCounterOrderId(), 0, 2);

        assertEquals(List.of(td.EXCHANGE_CONTRACT_2), contract);
    }

    @Test
    void testGetContractsForVersionedAccount() {
        List<ExchangeContract> contract = dao.getAllVersionedForAccountOrder(td.EXCHANGE_CONTRACT_4.getSender(), td.EXCHANGE_CONTRACT_4.getCounterOrderId(), 0, 2);

        assertEquals(List.of(td.EXCHANGE_CONTRACT_7, td.EXCHANGE_CONTRACT_5, td.EXCHANGE_CONTRACT_4), contract);
    }
}
