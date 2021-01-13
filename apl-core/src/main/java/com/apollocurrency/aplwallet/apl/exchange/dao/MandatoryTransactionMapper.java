/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransaction;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class MandatoryTransactionMapper implements RowMapper<MandatoryTransaction> {
    @Override
    public MandatoryTransaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        byte[] txBytes = rs.getBytes("transaction_bytes");
        byte[] requiredTxHash = rs.getBytes("required_tx_hash");
        long dbId = rs.getLong("db_id");
        return new MandatoryTransaction(requiredTxHash, txBytes, dbId);
    }
}
