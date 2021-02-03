package com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class KeyFactoryProducer {
    private static final LongKeyFactory<UnconfirmedTransactionEntity> unconfirmedTransactionKeyFactory = new LongKeyFactory<UnconfirmedTransactionEntity>("id") {
        @Override
        public DbKey newKey(UnconfirmedTransactionEntity unconfirmedTransaction) {
            return new LongKey(unconfirmedTransaction.getId());
        }
    };

    @Produces
    @Named("transactionKeyFactory")
    public LongKeyFactory<UnconfirmedTransactionEntity> createUnconfirmedTransactionKeyFactory() {
        return unconfirmedTransactionKeyFactory;
    }
}
