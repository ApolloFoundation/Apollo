package com.apollocurrency.aplwallet.apl.core.app;

public final class PollOptionResult {

    private long result;
    private long weight;

    PollOptionResult(long result, long weight) {
        this.result = result;
        this.weight = weight;
    }

    public long getResult() {
        return result;
    }

    public long getWeight() {
        return weight;
    }

    public void add(long vote, long weight) {
        this.result += vote;
        this.weight += weight;
    }

}
