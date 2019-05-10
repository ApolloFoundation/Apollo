/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.mapper;

import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollVoter;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingPollVoterMapper extends DerivedEntityMapper<PhasingPollVoter> {

    public PhasingPollVoterMapper(KeyFactory<PhasingPollVoter> keyFactory) {
        super(keyFactory);
    }

    @Override
    public PhasingPollVoter doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long pollId = rs.getLong("transaction_id");
        long voterId = rs.getLong("voter_id");
        return new PhasingPollVoter(null, null, pollId, voterId);
    }
}
