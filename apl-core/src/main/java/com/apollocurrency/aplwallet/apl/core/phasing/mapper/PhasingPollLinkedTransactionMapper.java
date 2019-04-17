/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.mapper;

import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollLinkedTransaction;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingPollLinkedTransactionMapper implements RowMapper<PhasingPollLinkedTransaction> {

    @Override
    public PhasingPollLinkedTransaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        int height = rs.getInt("height");
        long pollId = rs.getLong("transaction_id");
        long linkedTransactionId = rs.getLong("linked_transaction_id");
        byte[] fullHash = rs.getBytes("linked_full_hash");
        return new PhasingPollLinkedTransaction(pollId, linkedTransactionId, fullHash, height);
    }
}
