/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;

@Slf4j
public class TransactionDTOConverter implements Converter<UnconfirmedTransactionDTO, Transaction> {

    private final TransactionBuilderFactory builderFactory;

    @Inject
    public TransactionDTOConverter(TransactionBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    @Override
    public Transaction apply(UnconfirmedTransactionDTO txDto) {
        return builderFactory.newTransaction(txDto);
    }
}
