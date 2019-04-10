package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBI row mapper for {@link TransactionIndex}
 */
public class TransactionIndexRowMapper implements RowMapper<TransactionIndex> {

    @Override
    public TransactionIndex map(ResultSet rs, StatementContext ctx) throws SQLException {

        return TransactionIndex.builder()
                .transactionId(rs.getLong("transaction_id"))
                .partialTransactionHash(rs.getBytes("partial_transaction_hash"))
                .blockId(rs.getLong("block_id"))
                .build();
    }
}
