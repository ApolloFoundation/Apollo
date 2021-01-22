/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class TxReceiptMapper implements Converter<Transaction, TxReceipt> {
    private final BlockChainInfoService infoService;

    @Inject
    public TxReceiptMapper(BlockChainInfoService infoService) {
        this.infoService = infoService;
    }

    @Override
    public TxReceipt apply(Transaction model) {
        TxReceipt dto = new TxReceipt();
        dto.setTransaction(model.getStringId());
        dto.setAmount(String.valueOf(model.getAmountATM()));
        dto.setFee(String.valueOf(model.getFeeATM()));
        dto.setSender(Convert2.rsAccount(model.getSenderId()));
        if (model.getRecipientId() != 0) {
            dto.setRecipient(Convert2.rsAccount(model.getRecipientId()));
        }
        dto.setSignature(Convert.toHexString(model.getSignature().bytes()));
        dto.setTimestamp((long) model.getTimestamp());
        dto.setStatus(TxReceipt.StatusEnum.UNCONFIRMED);
        StringBuilder payload = new StringBuilder();
        TransactionUtils.convertAppendixToString(payload, model.getAttachment());
        TransactionUtils.convertAppendixToString(payload, model.getMessage());
        dto.setPayload(payload.toString());

        if (model.getBlock() != null) {
            dto.setHeight((long) model.getHeight());
            dto.setBlock(Long.toUnsignedString(model.getBlockId()));
            dto.setBlockTimestamp((long) model.getBlockTimestamp());
            dto.setIndex((int) model.getIndex());
            dto.setConfirmations((long) Math.max(0, infoService.getHeight() - model.getHeight()));
            dto.setStatus(dto.getConfirmations() > 0 ? TxReceipt.StatusEnum.CONFIRMED : TxReceipt.StatusEnum.UNCONFIRMED);
        } else {//unconfirmed transaction
            dto.setHeight(0L);
            dto.setIndex(-1);
            dto.setConfirmations(0L);
            dto.setStatus(TxReceipt.StatusEnum.UNCONFIRMED);
        }
        return dto;
    }
}
