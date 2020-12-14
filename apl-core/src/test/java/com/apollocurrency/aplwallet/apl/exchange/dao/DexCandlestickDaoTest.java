/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.DexTradingTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j

@Tag("slow")
class DexCandlestickDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/dex-candlestick-data.sql", null);
    private DexCandlestickDao dao;

    private DexTradingTestData td;

    @BeforeEach
    void setUp() {
        dao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(
            extension.getDatabaseManager().getJdbiHandleFactory(), DexCandlestickDao.class);
        td = new DexTradingTestData();
    }

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
    }

    @Test
    void testGetByTimestampAndCoin() {
        DexCandlestick ethCandlestick = dao.getByTimestamp(td.ETH_0_CANDLESTICK.getTimestamp(), DexCurrency.ETH);

        assertEquals(td.ETH_0_CANDLESTICK, ethCandlestick);
    }

    @Test
    void testGetByTimestampAndIncorrectCoin() {
        DexCandlestick candlestick = dao.getByTimestamp(td.PAX_4_CANDLESTICK.getTimestamp(), DexCurrency.ETH);

        assertNull(candlestick);
    }

    @Test
    void testGetForTimespan() {
        List<DexCandlestick> candlesticks = dao.getForTimespan(td.ETH_2_CANDLESTICK.getTimestamp(), td.ETH_6_CANDLESTICK.getTimestamp(), DexCurrency.ETH);

        assertEquals(List.of(td.ETH_2_CANDLESTICK, td.ETH_3_CANDLESTICK, td.ETH_4_CANDLESTICK, td.ETH_5_CANDLESTICK, td.ETH_6_CANDLESTICK), candlesticks);
    }

    @Test
    void testRemoveAll() {
        dao.removeAll();

        assertEquals(0, dao.getForTimespan(0, Integer.MAX_VALUE, DexCurrency.ETH).size());
    }

    @Test
    void testRemoveAfterTimestamp() {
        dao.removeAfterTimestamp(td.ETH_2_CANDLESTICK.getTimestamp());

        List<DexCandlestick> candlesticks = dao.getForTimespan(0, Integer.MAX_VALUE, DexCurrency.ETH);
        assertEquals(List.of(td.ETH_0_CANDLESTICK, td.ETH_1_CANDLESTICK, td.ETH_2_CANDLESTICK), candlesticks);
    }

    @Test
    void testGetLast() {
        DexCandlestick last = dao.getLast();

        assertEquals(td.ETH_9_CANDLESTICK, last);
    }


    @Test
    void testGetLastPax() {
        DexCandlestick last = dao.getLast(DexCurrency.PAX);

        assertEquals(td.PAX_4_CANDLESTICK, last);
    }

    @Test
    void testUpdate() {
        DexCandlestick candlestick = td.ETH_3_CANDLESTICK;
        candlestick.setClose(BigDecimal.valueOf(1, 7));
        candlestick.setOpen(BigDecimal.valueOf(0, 7));
        dao.update(candlestick);

        assertEquals(candlestick, dao.getByTimestamp(candlestick.getTimestamp(), candlestick.getCoin()));
    }

    @Test
    void testAdd() {
        DexCandlestick candlestick = new DexCandlestick(DexCurrency.ETH, BigDecimal.valueOf(10, 7), BigDecimal.valueOf(10, 7), BigDecimal.valueOf(10, 7), BigDecimal.valueOf(10, 7), BigDecimal.valueOf(1, 2), BigDecimal.valueOf(1, 7), td.ETH_9_CANDLESTICK.getTimestamp() + 1, td.ETH_9_CANDLESTICK.getTimestamp() + 1, td.ETH_9_CANDLESTICK.getTimestamp() + 1);

        dao.add(candlestick);

        assertEquals(candlestick, dao.getByTimestamp(candlestick.getTimestamp(), DexCurrency.ETH));
        assertEquals(11, dao.getForTimespan(0, Integer.MAX_VALUE, DexCurrency.ETH).size());
    }

    @Test
    void testGetLastBeforeTimestamp() {
        DexCandlestick last = dao.getLast(DexCurrency.PAX, td.PAX_3_CANDLESTICK.getTimestamp());

        assertEquals(td.PAX_2_CANDLESTICK, last);
    }

}
