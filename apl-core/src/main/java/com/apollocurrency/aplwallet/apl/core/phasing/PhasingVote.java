/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingVote {





    private final long phasedTransactionId;
    private final long voterId;
    private long voteId;

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
