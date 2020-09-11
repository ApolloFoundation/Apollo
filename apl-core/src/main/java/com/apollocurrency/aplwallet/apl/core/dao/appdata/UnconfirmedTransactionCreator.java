/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UnconfirmedTransactionCreator {
    private final TimeService timeService;

    @Inject
    public UnconfirmedTransactionCreator(TimeService timeService) {
        this.timeService = timeService;
    }

    public UnconfirmedTransaction from(Transaction transaction) {
        return new UnconfirmedTransaction(transaction, timeService.getEpochTime());
    }
}
