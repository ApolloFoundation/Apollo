/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates instances of the {@link TransactionConverter} using supplied config
 * @author Andrii Boiarskyi
 * @see TransactionConverter
 * @since 1.48.4
 */
@Singleton
public class TransactionConverterCreator {

    private final Blockchain blockchain;
    private final UnconfirmedTransactionConverterCreator uncTxConverterCreator;

    @Inject
    public TransactionConverterCreator(Blockchain blockchain, UnconfirmedTransactionConverterCreator uncTxConverterCreator) {
        this.blockchain = blockchain;
        this.uncTxConverterCreator = uncTxConverterCreator;
    }

    public TransactionConverter create(boolean priv) {
        UnconfirmedTransactionConverter unconfirmedTransactionConverter = uncTxConverterCreator.create(priv);
        TransactionConverter converter = new TransactionConverter(blockchain, unconfirmedTransactionConverter);
        converter.setPriv(priv);
        return converter;
    }
}
