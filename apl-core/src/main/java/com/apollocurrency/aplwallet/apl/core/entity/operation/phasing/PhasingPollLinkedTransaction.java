/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.operation.phasing;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;

import java.util.Arrays;
import java.util.Objects;

public class PhasingPollLinkedTransaction extends DerivedEntity {
    private long pollId;
    private long transactionId;
    private byte[] fullHash;


    public PhasingPollLinkedTransaction(Long dbId, Integer height, long pollId, long transactionId, byte[] fullHash) {
        super(dbId, height);
        this.pollId = pollId;
        this.transactionId = transactionId;
        this.fullHash = fullHash;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhasingPollLinkedTransaction)) return false;
        if (!super.equals(o)) return false;
        PhasingPollLinkedTransaction that = (PhasingPollLinkedTransaction) o;
        return Objects.equals(pollId, that.pollId) &&
            Objects.equals(transactionId, that.transactionId) &&
            Arrays.equals(fullHash, that.fullHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), pollId, transactionId);
        result = 31 * result + Arrays.hashCode(fullHash);
        return result;
    }

    public Long getPollId() {
        return pollId;
    }

    public void setPollId(Long pollId) {
        this.pollId = pollId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public byte[] getFullHash() {
        return fullHash;
    }

    public void setFullHash(byte[] fullHash) {
        this.fullHash = fullHash;
    }

}
