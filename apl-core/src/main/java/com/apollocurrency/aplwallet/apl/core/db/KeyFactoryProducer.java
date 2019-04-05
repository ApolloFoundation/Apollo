package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

public class KeyFactoryProducer {
    private static final LongKeyFactory<UnconfirmedTransaction> unconfirmedTransactionKeyFactory = new LongKeyFactory<UnconfirmedTransaction>("id") {
        @Override
        public DbKey newKey(UnconfirmedTransaction unconfirmedTransaction) {
            return new LongKey(unconfirmedTransaction.getTransaction().getId());
        }
    };

    @Produces
    @Named("transactionKeyFactory")
    public LongKeyFactory<UnconfirmedTransaction> createUnconfirmedTransactionKeyFactory() {
        return unconfirmedTransactionKeyFactory;
    }
}
