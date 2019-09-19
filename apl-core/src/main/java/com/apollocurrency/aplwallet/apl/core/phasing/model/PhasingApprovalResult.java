package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import lombok.Data;


@Data
public class PhasingApprovalResult extends DerivedEntity {
    private long phasingTx;
    private long approvedTx;

    public PhasingApprovalResult(Integer height, long phasingTx, long approvalTx) {
        super(null, height);
        this.phasingTx = phasingTx;
        this.approvedTx = approvalTx;
    }

    public PhasingApprovalResult(Long dbId, Integer height, long phasingTx, long approvedTx) {
        super(dbId, height);
        this.phasingTx = phasingTx;
        this.approvedTx = approvedTx;
    }
}
