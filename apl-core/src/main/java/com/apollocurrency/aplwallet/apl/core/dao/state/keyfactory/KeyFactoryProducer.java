package com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory;

import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class KeyFactoryProducer {
    private static final LongKeyFactory<UnconfirmedTransaction> unconfirmedTransactionKeyFactory = new LongKeyFactory<UnconfirmedTransaction>("id") {
        @Override
        public DbKey newKey(UnconfirmedTransaction unconfirmedTransaction) {
            return new LongKey(unconfirmedTransaction.getTransactionImpl().getId());
        }
    };

    @Produces
    @Named("transactionKeyFactory")
    public LongKeyFactory<UnconfirmedTransaction> createUnconfirmedTransactionKeyFactory() {
        return unconfirmedTransactionKeyFactory;
    }
}
