/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.mapper;

import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollVoter;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingPollVoterMapper implements RowMapper<PhasingPollVoter> {
    @Override
    public PhasingPollVoter map(ResultSet rs, StatementContext ctx) throws SQLException {
        long pollId = rs.getLong("transaction_id");
        long voterId = rs.getLong("voter_id");
        int height = rs.getInt("height");
        return new PhasingPollVoter(pollId, voterId, height);
    }
}
