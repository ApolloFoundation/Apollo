/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import com.apollocurrency.aplwallet.api.v2.model.TransactionInfoResp;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class TransactionInfoMapper implements Converter<Transaction, TransactionInfoResp> {
    private final Blockchain blockchain;

    @Inject
    public TransactionInfoMapper(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public TransactionInfoResp apply(Transaction model) {
        TransactionInfoResp dto = new TransactionInfoResp();
        dto.setId(model.getStringId());
        dto.setType(String.valueOf(model.getType().getType()));
        dto.setSubtype(String.valueOf(model.getType().getSubtype()));
        dto.setVersion(String.valueOf(model.getVersion()));
        dto.setAmount(String.valueOf(model.getAmountATM()));
        dto.setFee(String.valueOf(model.getFeeATM()));
        dto.setDeadline((int) model.getDeadline());
        dto.setSender(Convert2.rsAccount(model.getSenderId()));
        dto.setSenderPublicKey(Convert.toHexString(model.getSenderPublicKey()));
        if (model.getRecipientId() != 0) {
            dto.setRecipient(Convert2.rsAccount(model.getRecipientId()));
        }
        dto.setSignature(Convert.toHexString(model.getSignature()));
        dto.setSignatureHash(Convert.toHexString(Crypto.sha256().digest(model.getSignature())));
        dto.setFullHash(model.getFullHashString());
        dto.setReferencedTransactionFullHash(model.getReferencedTransactionFullHash());
        dto.setTimestamp((long) model.getTimestamp());
        if (model.getBlock() != null) {
            dto.setBlock(model.getBlock().getStringId());
            dto.setBlockTimestamp((long) model.getBlockTimestamp());
            dto.setEcBlockHeight((long) model.getECBlockHeight());
            dto.setEcBlockId(Long.toUnsignedString(model.getECBlockId()));
        }
        dto.setHeight((long) model.getHeight());
        dto.setIndex((int) model.getIndex());
        dto.setConfirmations(blockchain.getHeight() - model.getHeight());
        dto.setPhased(model.attachmentIsPhased());

        JSONObject attachmentJSON = new JSONObject();

        for (Appendix appendage : model.getAppendages(true)) {
            attachmentJSON.putAll(appendage.getJSONObject());
        }
        if (!attachmentJSON.isEmpty()) {
            for (Map.Entry entry : (Iterable<Map.Entry>) attachmentJSON.entrySet()) {
                if (entry.getValue() instanceof Long) {
                    entry.setValue(String.valueOf(entry.getValue()));
                }
            }
            dto.setAttachment(attachmentJSON);
        }
        return dto;
    }
}
