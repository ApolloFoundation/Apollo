/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShufflingProcessingPrunableLoader implements PrunableLoader<ShufflingProcessingAttachment> {
    private final PrunableService prunableService;
    private final ShufflingService shufflingService;

    @Inject
    public ShufflingProcessingPrunableLoader(PrunableService prunableService, ShufflingService shufflingService) {
        this.prunableService = prunableService;
        this.shufflingService = shufflingService;
    }

    @Override
    public void loadPrunable(Transaction transaction, ShufflingProcessingAttachment appendix, boolean includeExpiredPrunable) {
        if (appendix.getData() == null && prunableService.shouldLoadPrunable(transaction, includeExpiredPrunable)) {
            appendix.setData(shufflingService.getData(appendix.getShufflingId(), transaction.getSenderId()));
        }
    }

    @Override
    public void restorePrunableData(Transaction transaction, ShufflingProcessingAttachment appendix, int blockTimestamp, int height) {
        shufflingService.restoreData(appendix.getShufflingId(), transaction.getSenderId(), appendix.getData(), transaction.getTimestamp(), height);
    }
}
