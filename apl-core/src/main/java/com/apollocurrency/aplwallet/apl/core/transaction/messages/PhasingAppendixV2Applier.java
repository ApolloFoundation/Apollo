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
public class PhasingAppendixV2Applier implements AppendixApplier<PhasingAppendixV2> {
    private final PhasingPollService phasingPollService;

    @Inject
    public PhasingAppendixV2Applier(PhasingPollService phasingPollService) {
        this.phasingPollService = phasingPollService;
    }

    @Override
    public void apply(Transaction transaction, PhasingAppendixV2 appendix, Account senderAccount, Account recipientAccount) {
        phasingPollService.addPoll(transaction, appendix);
    }

    @Override
    public Class<PhasingAppendixV2> forClass() {
        return PhasingAppendixV2.class;
    }
}
