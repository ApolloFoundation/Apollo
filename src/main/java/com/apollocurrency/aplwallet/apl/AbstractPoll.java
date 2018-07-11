/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl;

import java.sql.ResultSet;
import java.sql.SQLException;

abstract class AbstractPoll {

    final long id;
    final VoteWeighting voteWeighting;
    final long accountId;
    final int finishHeight;

    AbstractPoll(long id, long accountId, int finishHeight, VoteWeighting voteWeighting) {
        this.id = id;
        this.accountId = accountId;
        this.finishHeight = finishHeight;
        this.voteWeighting = voteWeighting;
    }

    AbstractPoll(long id, long accountId, int finishHeight) {
        this.id = id;
        this.accountId = accountId;
        this.finishHeight = finishHeight;
        this.voteWeighting = new VoteWeighting((byte)0, 0L, 100_000_000L, (byte) 0);
    }

    AbstractPoll(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.finishHeight = rs.getInt("finish_height");
        this.voteWeighting = new VoteWeighting(rs.getByte("voting_model"), rs.getLong("holding_id"),
                rs.getLong("min_balance"), rs.getByte("min_balance_model"));
    }

    public final long getId() {
        return id;
    }

    public final long getAccountId() {
        return accountId;
    }

    public final int getFinishHeight() {
        return finishHeight;
    }

    public final VoteWeighting getVoteWeighting() {
        return voteWeighting;
    }

}

