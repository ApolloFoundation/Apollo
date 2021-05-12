/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BlockConverterCreator {
    private final AccountService accountService;
    private final PhasingPollService phasingPollService;
    private final Blockchain blockchain;
    private final PrunableLoadingService prunableLoadingService;

    @Inject
    public BlockConverterCreator(AccountService accountService, PhasingPollService phasingPollService, Blockchain blockchain, PrunableLoadingService prunableLoadingService) {
        this.accountService = accountService;
        this.phasingPollService = phasingPollService;
        this.blockchain = blockchain;
        this.prunableLoadingService = prunableLoadingService;
    }

    public BlockConverter create(boolean addTransactions, boolean addPhasedTransactions, boolean priv) {
        TransactionConverter transactionConverter = new TransactionConverter(blockchain, new UnconfirmedTransactionConverter(prunableLoadingService));
        BlockConverter blockConverter = new BlockConverter(blockchain, transactionConverter, phasingPollService, accountService);
        blockConverter.setAddTransactions(addTransactions);
        blockConverter.setAddPhasedTransactions(addPhasedTransactions);
        blockConverter.setPriv(priv);
        return blockConverter;
    }
    public BlockConverter create(boolean addTransactions, boolean addPhasedTransactions) {
        return create(addTransactions, addPhasedTransactions, true);
    }
}
