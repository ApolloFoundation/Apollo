package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DexCandlestickMapper implements RowMapper<DexCandlestick> {
    @Override
    public DexCandlestick map(ResultSet rs, StatementContext ctx) throws SQLException {
        DexCandlestick candlestick = new DexCandlestick();
        candlestick.setCoin(DexCurrency.getType(rs.getByte("coin")));
        candlestick.setOpen(rs.getBigDecimal("open"));
        candlestick.setClose(rs.getBigDecimal("close"));
        candlestick.setMin(rs.getBigDecimal("min"));
        candlestick.setMax(rs.getBigDecimal("max"));
        candlestick.setFromVolume(rs.getBigDecimal("from_volume"));
        candlestick.setToVolume(rs.getBigDecimal("to_volume"));
        candlestick.setTimestamp(rs.getInt("timestamp"));
        return candlestick;
    }
}
