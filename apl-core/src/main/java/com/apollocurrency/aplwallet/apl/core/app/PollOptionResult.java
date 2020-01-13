package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

public final class PollOptionResult {
    private long pollId;
    private Long result;
    private Long weight;
    private DbKey dbKey;

    public DbKey getDbKey() {
        return dbKey;
    }

    public boolean isUndefined() {
        return result == null && weight == null;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public long getResult() {
        return result;
    }

    public long getWeight() {
        return weight;
    }

    public long getPollId() {
        return pollId;
    }

    public PollOptionResult(long pollId) {
        this.pollId = pollId;
    }

    public void setPollId(long pollId) {
        this.pollId = pollId;
    }

    public PollOptionResult(long pollId, long result, long weight) {
        this.pollId = pollId;
        this.result = result;
        this.weight = weight;
    }

    public void add(long vote, long weight) {
        this.result += vote;
        this.weight += weight;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PollOptionResult{");
        sb.append("pollId=").append(pollId);
        sb.append(", result=").append(result);
        sb.append(", weight=").append(weight);
        sb.append('}');
        return sb.toString();
    }
}
