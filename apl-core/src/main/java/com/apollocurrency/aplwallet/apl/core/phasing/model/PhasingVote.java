/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PhasingVote {

    private final long phasedTransactionId;
    private final long voterId;
    private final long voteId;

    public PhasingVote(Transaction transaction, Account voter, long phasedTransactionId) {
        this.phasedTransactionId = phasedTransactionId;
        this.voterId = voter.getId();
        this.voteId = transaction.getId();
    }

    public PhasingVote(ResultSet rs) throws SQLException {
        this.phasedTransactionId = rs.getLong("transaction_id");
        this.voterId = rs.getLong("voter_id");
        this.voteId = rs.getLong("vote_id");
    }

    public PhasingVote(long phasedTransactionId, long voterId, long voteId) {
        this.phasedTransactionId = phasedTransactionId;
        this.voterId = voterId;
        this.voteId = voteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhasingVote)) return false;
        PhasingVote that = (PhasingVote) o;
        return phasedTransactionId == that.phasedTransactionId &&
                voterId == that.voterId &&
                voteId == that.voteId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(phasedTransactionId, voterId, voteId);
    }

    public long getPhasedTransactionId() {
        return phasedTransactionId;
    }

    public long getVoterId() {
        return voterId;
    }

    public long getVoteId() {
        return voteId;
    }

}
