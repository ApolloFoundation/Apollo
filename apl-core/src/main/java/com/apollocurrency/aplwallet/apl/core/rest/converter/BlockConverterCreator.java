/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BlockConverterCreator {
    private final PhasingPollService phasingPollService;
    private final TransactionConverterCreator txConverterCreator;
    private final Blockchain blockchain;

    @Inject
    public BlockConverterCreator(PhasingPollService phasingPollService, TransactionConverterCreator txConverterCreator, Blockchain blockchain) {
        this.phasingPollService = phasingPollService;
        this.txConverterCreator = txConverterCreator;
        this.blockchain = blockchain;
    }

    public BlockConverter create(boolean addTransactions, boolean addPhasedTransactions, boolean priv) {
        TransactionConverter transactionConverter = txConverterCreator.create(priv);
        BlockConverter blockConverter = new BlockConverter(blockchain, transactionConverter, phasingPollService);
        blockConverter.setAddTransactions(addTransactions);
        blockConverter.setAddPhasedTransactions(addPhasedTransactions);
        blockConverter.setPriv(priv);
        return blockConverter;
    }
    public BlockConverter create(boolean addTransactions, boolean addPhasedTransactions) {
        return create(addTransactions, addPhasedTransactions, true);
    }
}
