/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.poll;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(of = {"pollId", "result", "weight"})
public final class PollOptionResult {
    private long pollId;
    private Long result;
    private Long weight;
    private DbKey dbKey;
    private int height;

    public PollOptionResult(long pollId) {
        this.pollId = pollId;
    }

    public PollOptionResult(long pollId, int height) {
        this.pollId = pollId;
        this.height = height;
    }

    public PollOptionResult(long pollId, long result, long weight) {
        this.pollId = pollId;
        this.result = result;
        this.weight = weight;
    }

    public PollOptionResult(long pollId, long result, long weight, int height) {
        this.pollId = pollId;
        this.result = result;
        this.weight = weight;
        this.height = height;
    }

    public boolean isUndefined() {
        return result == null && weight == null;
    }

    public void add(long vote, long weight) {
        this.result += vote;
        this.weight += weight;
    }
}