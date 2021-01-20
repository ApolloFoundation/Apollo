/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class PrunableTxRowMapper implements RowMapper<PrunableTransaction> {
    private final TransactionTypeFactory typeFactory;

    @Inject
    public PrunableTxRowMapper(TransactionTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    @Override
    public PrunableTransaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        try {
            return parseTxReceipt(rs);
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private PrunableTransaction parseTxReceipt(ResultSet rs) throws AplException.NotValidException {
        try {
            long id = rs.getLong("id");
            byte type = rs.getByte("type");
            byte subtype = rs.getByte("subtype");
            TransactionType transactionType = typeFactory.findTransactionType(type, subtype);
            if (transactionType == null) {
                throw new AplException.NotValidException("Wrong transaction type/subtype value, type=" + type + " subtype=" + subtype);
            }
            PrunableTransaction transaction = new PrunableTransaction(id, transactionType,
                rs.getBoolean("prunable_attachment"),
                rs.getBoolean("prunable_plain_message"),
                rs.getBoolean("prunable_encrypted_message"));

            return transaction;

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
