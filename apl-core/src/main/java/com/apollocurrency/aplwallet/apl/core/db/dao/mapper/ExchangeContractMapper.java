package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExchangeContractMapper  implements RowMapper<ExchangeContract> {
    @Override
    public ExchangeContract map(ResultSet rs, StatementContext ctx) throws SQLException {
        return ExchangeContract.builder()
                .orderId(rs.getLong("db_id"))
                .orderId(rs.getLong("order_id"))
                .counterOrderId(rs.getLong("counter_order_id"))
                .secretHash(rs.getString("secret_hash"))
                .build();
    }
}