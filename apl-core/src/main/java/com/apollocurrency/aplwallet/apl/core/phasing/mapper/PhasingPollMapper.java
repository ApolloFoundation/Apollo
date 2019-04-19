/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.mapper;

import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingCreator;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingPollMapper extends DerivedEntityMapper<PhasingPoll> {

    @Override
    public PhasingPoll doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long accountId = rs.getLong("account_id");
        int finishHeight = rs.getInt("finish_height");
        byte votingModel = rs.getByte("voting_model");
        long holdingId = rs.getLong("holding_id");
        long minBalance = rs.getLong("min_balance");
        byte minBalanceModel = rs.getByte("min_balance_model");
        long quorum = rs.getLong("quorum");
        byte[] hashedSecret = rs.getBytes("hashed_secret");
        byte algorithm = rs.getByte("algorithm");
        byte whiteListSize = rs.getByte("whitelist_size");
        return PhasingCreator.createPoll(id, accountId, whiteListSize, finishHeight, votingModel, quorum, minBalance, holdingId, minBalanceModel, hashedSecret, algorithm);
    }
}
