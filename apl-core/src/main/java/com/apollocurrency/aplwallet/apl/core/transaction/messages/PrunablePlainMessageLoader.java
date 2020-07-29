/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PrunablePlainMessageLoader implements PrunableLoader<PrunablePlainMessageAppendix>  {
    private final PrunableService prunableService;
    private final PrunableMessageService messageService;

    @Inject
    public PrunablePlainMessageLoader(PrunableService prunableService, PrunableMessageService messageService) {
        this.prunableService = prunableService;
        this.messageService = messageService;
    }

    @Override
    public void loadPrunable(Transaction transaction, PrunablePlainMessageAppendix appendix, boolean includeExpiredPrunable) {
        if (!appendix.hasPrunableData() && prunableService.shouldLoadPrunable(transaction, includeExpiredPrunable)) {
            PrunableMessage prunableMessage = messageService.get(transaction.getId());
            if (prunableMessage != null && prunableMessage.getMessage() != null) {
                appendix.setPrunableMessage(prunableMessage);
            }
        }
    }

    @Override
    public void restorePrunableData(Transaction transaction, PrunablePlainMessageAppendix appendix, int blockTimestamp, int height) {
        messageService.add(transaction, appendix, blockTimestamp, height);
    }
}
