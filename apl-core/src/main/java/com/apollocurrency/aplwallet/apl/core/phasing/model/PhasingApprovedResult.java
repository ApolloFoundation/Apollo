package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode
public class PhasingApprovedResult extends DerivedEntity {
    private final long phasingTx;
    private final long approvedTx;

    public PhasingApprovedResult(long phasingTx, long approvalTx) {
        super(null, null);
        this.phasingTx = phasingTx;
        this.approvedTx = approvalTx;
    }

}
