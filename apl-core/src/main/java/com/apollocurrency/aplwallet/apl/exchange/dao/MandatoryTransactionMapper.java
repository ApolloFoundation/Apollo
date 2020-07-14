/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class MandatoryTransactionMapper implements RowMapper<MandatoryTransaction> {
    @Override
    public MandatoryTransaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        byte[] txBytes = rs.getBytes("transaction_bytes");
        Transaction tx;
        try {
            tx = Transaction.newTransactionBuilder(txBytes).build();
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e);
        }
        byte[] requiredTxHash = rs.getBytes("required_tx_hash");
        long dbId = rs.getLong("db_id");
        return new MandatoryTransaction(tx, requiredTxHash, dbId);
    }
}
