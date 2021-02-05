/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class TransactionDTOConverter implements Converter<TransactionDTO, Transaction> {

    private final TransactionBuilderFactory builderFactory;

    @Inject
    public TransactionDTOConverter(TransactionBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    @Override
    public Transaction apply(TransactionDTO txDto) {
        return builderFactory.newTransaction(txDto);
    }
}
