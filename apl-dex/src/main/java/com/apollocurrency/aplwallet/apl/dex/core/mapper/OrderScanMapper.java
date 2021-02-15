package com.apollocurrency.aplwallet.apl.dex.core.mapper;

import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderScan;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderScanMapper implements RowMapper<OrderScan> {
    @Override
    public OrderScan map(ResultSet rs, StatementContext ctx) throws SQLException {
        byte coin = rs.getByte("coin");
        long lastDbId = rs.getLong("last_db_id");
        return new OrderScan(DexCurrency.getType(coin), lastDbId);
    }
}
