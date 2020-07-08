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
    public TransactionInfoResp apply(Transaction transaction) {
        TransactionInfoResp o = new TransactionInfoResp();
        o.setId(transaction.getStringId());
        o.setType(String.valueOf(transaction.getType().getType()));
        o.setSubtype(String.valueOf(transaction.getType().getSubtype()));
        o.setVersion(String.valueOf(transaction.getVersion()));
        o.setAmount(String.valueOf(transaction.getAmountATM()));
        o.setFee(String.valueOf(transaction.getFeeATM()));
        o.setDeadline((int) transaction.getDeadline());
        o.setSender(Convert2.rsAccount(transaction.getSenderId()));
        o.setSenderPublicKey(Convert.toHexString(transaction.getSenderPublicKey()));
        if (transaction.getRecipientId() != 0) {
            o.setRecipient(Convert2.rsAccount(transaction.getRecipientId()));
        }
        o.setSignature(Convert.toHexString(transaction.getSignature()));
        o.setSignatureHash(Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
        o.setFullHash(transaction.getFullHashString());
        o.setReferencedTransactionFullHash(transaction.getReferencedTransactionFullHash());
        o.setTimestamp((long) transaction.getTimestamp());
        if (transaction.getBlock() != null) {
            o.setBlock(transaction.getBlock().getStringId());
            o.setBlockTimestamp((long) transaction.getBlockTimestamp());
            o.setEcBlockHeight((long) transaction.getECBlockHeight());
            o.setEcBlockId(Long.toUnsignedString(transaction.getECBlockId()));
        }
        o.setHeight((long) transaction.getHeight());
        o.setIndex((int) transaction.getIndex());
        o.setConfirmations(blockchain.getHeight() - transaction.getHeight());
        o.setPhased(transaction.attachmentIsPhased());

        JSONObject attachmentJSON = new JSONObject();

        for (Appendix appendage : transaction.getAppendages(true)) {
            attachmentJSON.putAll(appendage.getJSONObject());
        }
        if (!attachmentJSON.isEmpty()) {
            for (Map.Entry entry : (Iterable<Map.Entry>) attachmentJSON.entrySet()) {
                if (entry.getValue() instanceof Long) {
                    entry.setValue(String.valueOf(entry.getValue()));
                }
            }
            o.setAttachment(attachmentJSON);
        }
        return o;
    }
}
