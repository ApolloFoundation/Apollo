/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PhasingAppendixApplier implements AppendixApplier<PhasingAppendix> {
    private final PhasingPollService phasingPollService;

    @Inject
    public PhasingAppendixApplier(PhasingPollService phasingPollService) {
        this.phasingPollService = phasingPollService;
    }

    @Override
    public void apply(Transaction transaction, PhasingAppendix appendix, Account senderAccount, Account recipientAccount) {
        phasingPollService.addPoll(transaction, appendix);
    }

    @Override
    public Class<PhasingAppendix> forClass() {
        return PhasingAppendix.class;
    }
}
