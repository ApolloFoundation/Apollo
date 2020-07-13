/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.phasing;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.converter.db.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.model.PhasingCreator;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingPollMapper extends DerivedEntityMapper<PhasingPoll> {

    public PhasingPollMapper(KeyFactory<PhasingPoll> keyFactory) {
        super(keyFactory);
    }

    @Override
    public PhasingPoll doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long accountId = rs.getLong("account_id");
        int finishHeight = rs.getInt("finish_height");
        int finishTime = rs.getInt("finish_time");
        byte votingModel = rs.getByte("voting_model");
        long holdingId = rs.getLong("holding_id");
        long minBalance = rs.getLong("min_balance");
        byte minBalanceModel = rs.getByte("min_balance_model");
        long quorum = rs.getLong("quorum");
        byte[] hashedSecret = rs.getBytes("hashed_secret");
        byte algorithm = rs.getByte("algorithm");
        byte whiteListSize = rs.getByte("whitelist_size");

        return PhasingCreator.createPoll(id, accountId, whiteListSize, finishHeight, finishTime, votingModel, quorum,
            minBalance, holdingId, minBalanceModel, hashedSecret, algorithm);
    }
}
