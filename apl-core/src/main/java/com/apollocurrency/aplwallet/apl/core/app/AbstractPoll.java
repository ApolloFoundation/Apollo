/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public abstract class AbstractPoll extends DerivedEntity {

    final long id;
    protected final VoteWeighting voteWeighting;
    final long accountId;
    protected final int finishHeight;

    public AbstractPoll(Long dbId, Integer height, long id, VoteWeighting voteWeighting, long accountId, int finishHeight) {
        super(dbId, height);
        this.id = id;
        this.voteWeighting = voteWeighting;
        this.accountId = accountId;
        this.finishHeight = finishHeight;
    }

    public AbstractPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.finishHeight = rs.getInt("finish_height");
        this.voteWeighting = new VoteWeighting(rs.getByte("voting_model"), rs.getLong("holding_id"),
                rs.getLong("min_balance"), rs.getByte("min_balance_model"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractPoll)) return false;
        if (!super.equals(o)) return false;
        AbstractPoll that = (AbstractPoll) o;
        return id == that.id &&
                accountId == that.accountId &&
                finishHeight == that.finishHeight &&
                Objects.equals(voteWeighting, that.voteWeighting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, voteWeighting, accountId, finishHeight);
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AbstractPoll{");
        sb.append("id=").append(id);
        sb.append(", voteWeighting=").append(voteWeighting);
        sb.append(", accountId=").append(accountId);
        sb.append(", finishHeight=").append(finishHeight);
        sb.append('}');
        return sb.toString();
    }
}

