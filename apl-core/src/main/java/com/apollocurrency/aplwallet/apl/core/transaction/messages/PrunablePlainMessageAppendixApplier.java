/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class PrunablePlainMessageAppendixApplier implements AppendixApplier<PrunablePlainMessageAppendix> {
    private final TimeService timeService;
    private final BlockchainConfig blockchainConfig;
    private final PrunableMessageService messageService;

    @Inject
    public PrunablePlainMessageAppendixApplier(TimeService timeService, BlockchainConfig blockchainConfig, PrunableMessageService messageService) {
        this.timeService = timeService;
        this.blockchainConfig = blockchainConfig;
        this.messageService = messageService;
    }

    @Override
    public void apply(Transaction transaction, PrunablePlainMessageAppendix appendix, Account senderAccount, Account recipientAccount) {
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime()) {
            messageService.add(transaction, appendix);
        }
    }

    @Override
    public Class<PrunablePlainMessageAppendix> forClass() {
        return PrunablePlainMessageAppendix.class;
    }
}
