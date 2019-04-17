/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.util.Objects;

public class PhasingVote {

    private DbKey dbKey;
    private final long phasedTransactionId;
    private final long voterId;
    private final long voteId;
    private final int height;

    public PhasingVote(Transaction transaction, Account voter, long phasedTransactionId) {
        this.phasedTransactionId = phasedTransactionId;
        this.voterId = voter.getId();
        this.voteId = transaction.getId();
        this.height = transaction.getHeight();
    }



    public PhasingVote(long phasedTransactionId, long voterId, long voteId, int height) {
        this.phasedTransactionId = phasedTransactionId;
        this.voterId = voterId;
        this.voteId = voteId;
        this.height = height;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhasingVote)) return false;
        PhasingVote that = (PhasingVote) o;
        return phasedTransactionId == that.phasedTransactionId &&
                voterId == that.voterId &&
                voteId == that.voteId &&
                height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(phasedTransactionId, voterId, voteId, height);
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
