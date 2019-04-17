/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.util.Objects;

public class PhasingPollVoter {
    private DbKey dbKey;
    private Long pollId;
    private Long voterId;
    private int height;

    public PhasingPollVoter(Long pollId, Long voterId, int height) {
        this.pollId = pollId;
        this.voterId = voterId;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhasingPollVoter)) return false;
        PhasingPollVoter that = (PhasingPollVoter) o;
        return height == that.height &&
                Objects.equals(pollId, that.pollId) &&
                Objects.equals(voterId, that.voterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pollId, voterId, height);
    }

    public Long getPollId() {
        return pollId;
    }

    public void setPollId(Long pollId) {
        this.pollId = pollId;
    }

    public Long getVoterId() {
        return voterId;
    }

    public void setVoterId(Long voterId) {
        this.voterId = voterId;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }
}
