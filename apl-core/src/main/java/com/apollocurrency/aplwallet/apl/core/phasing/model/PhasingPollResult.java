/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class PhasingPollResult extends DerivedEntity {

    private final long id;
    private final long result;
    private final boolean approved;

    public PhasingPollResult(PhasingPoll poll, long result, int height) {
        super(poll.getDbId(), height);
        this.id = poll.getId();
        this.result = result;
        this.approved = result >= poll.getQuorum();
    }

    public PhasingPollResult(Long dbId, Integer height, long id, long result, boolean approved) {
        super(dbId, height);
        this.id = id;
        this.result = result;
        this.approved = approved;
    }

}
