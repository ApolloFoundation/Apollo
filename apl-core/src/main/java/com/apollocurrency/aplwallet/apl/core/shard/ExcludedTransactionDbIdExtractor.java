/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ExcludedTransactionDbIdExtractor {
    private final PhasingPollService phasingPollService;

    @Inject
    public ExcludedTransactionDbIdExtractor(PhasingPollService phasingPollService) {
        this.phasingPollService = phasingPollService;
    }

    public List<Long> getDbIds(int height) {
        return phasingPollService.getActivePhasedTransactionDbIdsAtHeight(height);
    }
}
