/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import javax.inject.Inject;

public class TransactionConverter implements Converter<Transaction, TransactionDTO> {

    private final Blockchain blockchain;
    private final UnconfirmedTransactionConverter unconfirmedTransactionConverter;

    @Inject
    public TransactionConverter(Blockchain blockchain, UnconfirmedTransactionConverter unconfirmedTransactionConverter) {
        this.blockchain = blockchain;
        this.unconfirmedTransactionConverter = unconfirmedTransactionConverter;
    }

    @Override
    public TransactionDTO apply(Transaction model) {
        TransactionDTO dto = new TransactionDTO(unconfirmedTransactionConverter.convert(model));
        dto.setBlock(Long.toUnsignedString(model.getBlockId()));
        dto.setConfirmations(blockchain.getHeight() - model.getHeight());
        dto.setBlockTimestamp(model.getBlockTimestamp());
        dto.setTransactionIndex(model.getIndex());
        dto.setErrorMessage(model.getErrorMessage().orElse(null));
        return dto;
    }

    public void setPriv(boolean priv) {
        unconfirmedTransactionConverter.setPriv(priv);
    }
}
