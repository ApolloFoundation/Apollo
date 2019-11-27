package com.apollocurrency.aplwallet.apl.exchange.dao;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.DexTradingTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

class DexCandlestickDaoTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/dex-candlestick-data.sql", null);
    private DexCandlestickDao dao;

    private DexTradingTestData td;
    @BeforeEach
    void setUp() {
        dao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(
                extension.getDatabaseManager().getJdbiHandleFactory(), DexCandlestickDao.class);
        td = new DexTradingTestData();
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
    void testGetFromToTimestamp() {
        List<DexCandlestick> candlesticks = dao.getFromToTimestamp(td.ETH_2_CANDLESTICK.getTimestamp(), td.ETH_6_CANDLESTICK.getTimestamp(), DexCurrency.ETH);

        assertEquals(List.of(td.ETH_2_CANDLESTICK, td.ETH_3_CANDLESTICK, td.ETH_4_CANDLESTICK, td.ETH_5_CANDLESTICK), candlesticks);
    }

    @Test
    void testRemoveAll() {
        dao.removeAll();

        assertEquals(0, dao.getFromToTimestamp(0, Integer.MAX_VALUE, DexCurrency.ETH).size());
    }

    @Test
    void testRemoveAfterTimestamp() {
        dao.removeAfterTimestamp(td.ETH_2_CANDLESTICK.getTimestamp());

        List<DexCandlestick> candlesticks = dao.getFromToTimestamp(0, Integer.MAX_VALUE, DexCurrency.ETH);
        assertEquals(List.of(td.ETH_0_CANDLESTICK, td.ETH_1_CANDLESTICK, td.ETH_2_CANDLESTICK), candlesticks);
    }

}
