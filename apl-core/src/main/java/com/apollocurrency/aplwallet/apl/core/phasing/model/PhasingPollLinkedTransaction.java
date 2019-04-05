/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.util.Arrays;
import java.util.Objects;

public class PhasingPollLinkedTransaction {
    private DbKey dbKey;
    private Long pollId;
    private Long transactionId;
    private byte[] fullHash;
    private int height;

    public PhasingPollLinkedTransaction(Long pollId, Long transactionId, byte[] fullHash, int height) {
        this.pollId = pollId;
        this.transactionId = transactionId;
        this.fullHash = fullHash;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhasingPollLinkedTransaction)) return false;
        PhasingPollLinkedTransaction that = (PhasingPollLinkedTransaction) o;
        return height == that.height &&
                Objects.equals(dbKey, that.dbKey) &&
                Objects.equals(pollId, that.pollId) &&
                Objects.equals(transactionId, that.transactionId) &&
                Arrays.equals(fullHash, that.fullHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(dbKey, pollId, transactionId, height);
        result = 31 * result + Arrays.hashCode(fullHash);
        return result;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
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

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
