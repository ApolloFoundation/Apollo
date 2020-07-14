/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.TransactionIndex;
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
            .height(rs.getInt("height"))
            .transactinIndex(rs.getShort("transaction_index"))
            .build();
    }
}
