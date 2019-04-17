/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.mapper;


import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingVoteMapper implements RowMapper<PhasingVote> {
    @Override
    public PhasingVote map(ResultSet rs, StatementContext ctx) throws SQLException {
        long phasedTransactionId = rs.getLong("transaction_id");
        long voterId = rs.getLong("voter_id");
        long voteId = rs.getLong("vote_id");
        int height = rs.getInt("height");
        return new PhasingVote(phasedTransactionId, voterId, voteId, height);
    }
}
