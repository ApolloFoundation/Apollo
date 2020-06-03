/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.ReferencedTransaction;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ReferencedTransactionRowMapper implements RowMapper<ReferencedTransaction> {

    @Override
    public ReferencedTransaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        long dbId = rs.getLong("db_id");
        long transactionId = rs.getLong("transaction_id");
        long referencedTransactionId = rs.getLong("referenced_transaction_id");
        int height = rs.getInt("height");
        return new ReferencedTransaction(dbId, transactionId, referencedTransactionId, height);
    }
}
