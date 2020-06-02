/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.operation.phasing;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;

public class PhasingVote extends DerivedEntity {

    private final long phasedTransactionId;
    private final long voterId;
    private final long voteId;


    public PhasingVote(Long dbId, Integer height, long phasedTransactionId, long voterId, long voteId) {
        super(dbId, height);
        this.phasedTransactionId = phasedTransactionId;
        this.voterId = voterId;
        this.voteId = voteId;
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
