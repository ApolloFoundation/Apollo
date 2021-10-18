/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates instances of the {@link UnconfirmedTransactionConverter} according to the supplied configuration
 * @author Andrii Boiarskyi
 * @see UnconfirmedTransactionConverter
 * @since 1.48.4
 */
@Singleton
public class UnconfirmedTransactionConverterCreator {
    private final PrunableLoadingService prunableLoadingService;


    @Inject
    public UnconfirmedTransactionConverterCreator(PrunableLoadingService prunableLoadingService) {
        this.prunableLoadingService = prunableLoadingService;
    }

    public UnconfirmedTransactionConverter create(boolean priv) {
        UnconfirmedTransactionConverter converter = new UnconfirmedTransactionConverter(prunableLoadingService);
        converter.setPriv(priv);
        return converter;
    }
}
