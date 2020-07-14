/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.phasing;

import com.apollocurrency.aplwallet.apl.core.converter.db.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollVoter;
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
