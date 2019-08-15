package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode
public class PhasingApprovalResult extends DerivedEntity {
    private final long phasingTx;
    private final long approvedTx;

    public PhasingApprovalResult(Integer height, long phasingTx, long approvalTx) {
        super(null, height);
        this.phasingTx = phasingTx;
        this.approvedTx = approvalTx;
    }

}
