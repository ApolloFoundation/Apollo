/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShufflingProcessingPrunableLoader implements PrunableLoader<ShufflingProcessingAttachment> {
    private final PrunableLoadingChecker loadingChecker;
    private final ShufflingService shufflingService;

    @Inject
    public ShufflingProcessingPrunableLoader(PrunableLoadingChecker loadingChecker, ShufflingService shufflingService) {
        this.loadingChecker = loadingChecker;
        this.shufflingService = shufflingService;
    }

    @Override
    public void loadPrunable(Transaction transaction, ShufflingProcessingAttachment appendix, boolean includeExpiredPrunable) {
        if (appendix.getData() == null && loadingChecker.shouldLoadPrunable(transaction, includeExpiredPrunable)) {
            appendix.setData(shufflingService.getData(transaction.getId()));
        }
    }

    @Override
    public void restorePrunableData(Transaction transaction, ShufflingProcessingAttachment appendix, int blockTimestamp, int height) {
        shufflingService.restoreData(transaction.getId(), appendix.getShufflingId(), transaction.getSenderId(), appendix.getData(), transaction.getTimestamp(), height);
    }

    @Override
    public Class<ShufflingProcessingAttachment> forClass() {
        return ShufflingProcessingAttachment.class;
    }
}
