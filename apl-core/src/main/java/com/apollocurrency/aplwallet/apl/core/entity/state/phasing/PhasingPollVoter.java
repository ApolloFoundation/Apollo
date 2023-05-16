/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.phasing;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;

import java.util.Objects;

public class PhasingPollVoter extends DerivedEntity {
    private long pollId;
    private long voterId;

    public PhasingPollVoter(Long dbId, Integer height, long pollId, long voterId) {
        super(dbId, height);
        this.pollId = pollId;
        this.voterId = voterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhasingPollVoter)) return false;
        if (!super.equals(o)) return false;
        PhasingPollVoter that = (PhasingPollVoter) o;
        return Objects.equals(pollId, that.pollId) &&
            Objects.equals(voterId, that.voterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pollId, voterId);
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

}
