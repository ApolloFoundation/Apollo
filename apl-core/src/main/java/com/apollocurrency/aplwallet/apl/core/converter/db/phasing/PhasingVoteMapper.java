/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.phasing;


import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.converter.db.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingVote;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingVoteMapper extends DerivedEntityMapper<PhasingVote> {

    public PhasingVoteMapper(KeyFactory<PhasingVote> keyFactory) {
        super(keyFactory);
    }

    @Override
    public PhasingVote doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long phasedTransactionId = rs.getLong("transaction_id");
        long voterId = rs.getLong("voter_id");
        long voteId = rs.getLong("vote_id");
        return new PhasingVote(null, null, phasedTransactionId, voterId, voteId);
    }
}
