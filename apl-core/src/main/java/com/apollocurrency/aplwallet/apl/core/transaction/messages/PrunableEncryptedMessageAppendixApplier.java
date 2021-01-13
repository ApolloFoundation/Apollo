/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PrunableEncryptedMessageAppendixApplier implements AppendixApplier<PrunableEncryptedMessageAppendix> {
    private final TimeService timeService;
    private final BlockchainConfig blockchainConfig;
    private final PrunableMessageService messageService;

    @Inject
    public PrunableEncryptedMessageAppendixApplier(TimeService timeService, BlockchainConfig blockchainConfig, PrunableMessageService messageService) {
        this.timeService = timeService;
        this.blockchainConfig = blockchainConfig;
        this.messageService = messageService;
    }


    @Override
    public void apply(Transaction transaction, PrunableEncryptedMessageAppendix appendix, Account senderAccount, Account recipientAccount) {
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime()) {
            messageService.add(transaction, appendix);
        }
    }
}
