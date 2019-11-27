package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.mapper.DexCandlestickMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface DexCandlestickDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_candlestick WHERE coin = :pairedCoin AND timestamp < : toTimestamp ORDER BY timestamp DESC LIMIT :limit")
    @RegisterRowMapper(DexCandlestickMapper.class)
    List<DexCandlestick> getToTimestamp(@Bind("toTimestamp") int toTimestamp, @Bind("limit") int limit, @Bind("pairedCoin") DexCurrency pairedCoin);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_candlestick WHERE coin = :pairedCoin AND timestamp = :timestamp")
    @RegisterRowMapper(DexCandlestickMapper.class)
    DexCandlestick getByTimestamp(@Bind("timestamp") int timestamp, @Bind("pairedCoin") DexCurrency pairedCoin);

    @Transactional
    @SqlUpdate("DELETE FROM dex_candlestick")
    int removeAll();

    @Transactional
    @SqlUpdate("DELETE FROM dex_candlestick WHERE timestamp > :timestamp")
    int removeAfterTimestamp(@Bind("timestamp") int timestamp);

    @Transactional
    @SqlUpdate("INSERT INTO dex_candlestick(coin, min, max, open, close, from_volume,to_volume, timestamp) VALUES (:coin, :min, :max, :open, :close, :fromVolume, :toVolume, :timestamp)")
    void add(DexCandlestick candlestick);
}
