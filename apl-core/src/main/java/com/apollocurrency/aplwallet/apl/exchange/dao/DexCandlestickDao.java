package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.exchange.mapper.DexCandlestickMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface DexCandlestickDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_candlestick WHERE coin = :pairedCoin AND `timestamp` BETWEEN :fromTimestamp AND :toTimestamp ORDER BY `timestamp`")
    @RegisterRowMapper(DexCandlestickMapper.class)
    List<DexCandlestick> getForTimespan(@Bind("fromTimestamp") int fromTimestamp, @Bind("toTimestamp") int toTimestamp, @Bind("pairedCoin") DexCurrency pairedCoin);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_candlestick WHERE coin = :pairedCoin AND `timestamp` = :timestamp")
    @RegisterRowMapper(DexCandlestickMapper.class)
    DexCandlestick getByTimestamp(@Bind("timestamp") int timestamp, @Bind("pairedCoin") DexCurrency pairedCoin);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_candlestick WHERE coin = :pairedCoin ORDER BY `timestamp` DESC LIMIT 1")
    @RegisterRowMapper(DexCandlestickMapper.class)
    DexCandlestick getLast(@Bind("pairedCoin") DexCurrency pairedCoin);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_candlestick ORDER BY `timestamp` DESC LIMIT 1")
    @RegisterRowMapper(DexCandlestickMapper.class)
    DexCandlestick getLast();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_candlestick WHERE coin = :pairedCoin AND `timestamp` < :timestamp ORDER BY `timestamp` DESC LIMIT 1")
    @RegisterRowMapper(DexCandlestickMapper.class)
    DexCandlestick getLast(@Bind("pairedCoin") DexCurrency pairedCoin, @Bind("timestamp") int beforeTimestamp);

    @Transactional
    @SqlUpdate("DELETE FROM dex_candlestick")
    int removeAll();

    @Transactional
    @SqlUpdate("DELETE FROM dex_candlestick WHERE `timestamp` > :timestamp")
    int removeAfterTimestamp(@Bind("timestamp") int timestamp);

    @Transactional
    @SqlUpdate("INSERT INTO dex_candlestick(coin, min, max, open, close, from_volume,to_volume, `timestamp`, open_order_timestamp, close_order_timestamp) VALUES (:coin, :min, :max, :open, :close, :fromVolume, :toVolume, :timestamp, :openOrderTimestamp, :closeOrderTimestamp)")
    void add(@BindBean DexCandlestick candlestick);

    @Transactional
    @SqlUpdate("UPDATE dex_candlestick SET min = :min, max = :max, open = :open, close = :close, from_volume = :fromVolume, to_volume = :toVolume, open_order_timestamp =:openOrderTimestamp, close_order_timestamp =:closeOrderTimestamp WHERE `timestamp` = :timestamp AND coin = :coin")
    void update(@BindBean DexCandlestick candlestick);

}
