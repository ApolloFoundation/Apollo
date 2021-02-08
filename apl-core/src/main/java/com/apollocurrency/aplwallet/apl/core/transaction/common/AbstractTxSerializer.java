/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.io.ByteArrayStream;
import com.apollocurrency.aplwallet.apl.core.io.JsonBuffer;
import com.apollocurrency.aplwallet.apl.core.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.core.io.Result;
import com.apollocurrency.aplwallet.apl.core.io.WriteBuffer;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractTxSerializer implements TxSerializer {

    @Getter
    private final TxBContextImpl context;

    @Getter
    private WriteBuffer buffer;

    protected AbstractTxSerializer(TxBContextImpl context) {
        Objects.requireNonNull(context);
        this.context = context;
    }

    public abstract int write(Transaction transaction, WriteBuffer buffer);

    @Override
    public void serialize(Transaction transaction, Result result) {
        this.buffer = createBuffer(result);
        write(transaction, result);
    }

    private WriteBuffer createBuffer(Result result) {
        WriteBuffer writeBuffer;
        // use context properties
        if (result instanceof PayloadResult) {
            writeBuffer = ((PayloadResult) result).getBuffer();
        } else {
            writeBuffer = new ByteArrayStream();
        }
        return writeBuffer;
    }

    protected void write(Transaction transaction, Result result) {
        try {
            if (buffer instanceof JsonBuffer) {
                write(transaction, ((JsonBuffer) buffer));
            } else {
                int payloadSize = write(transaction, buffer);
                if (result instanceof PayloadResult) {
                    ((PayloadResult) result).setPayloadSize(payloadSize);
                }
            }
        } catch (RuntimeException e) {
            if (transaction.getSignature() != null && log.isDebugEnabled()) {
                log.debug("Failed to get transaction bytes for transaction id={}, height={}", transaction.getId(), transaction.getHeight());
            }
            throw e;
        }
    }

    protected void write(Transaction transaction, JsonBuffer json) {
        json.put("id", Long.toUnsignedString(transaction.getId()));
        json.put("version", transaction.getVersion());
        TransactionType type = transaction.getType();
        TransactionTypes.TransactionTypeSpec spec = type.getSpec();
        json.put("type", spec.getType());
        json.put("subtype", spec.getSubtype());
        json.put("timestamp", transaction.getTimestamp());
        json.put("deadline", transaction.getDeadline());
        json.put("senderPublicKey", Convert.toHexString(transaction.getSenderPublicKey()));
        if (type.canHaveRecipient()) {
            json.put("recipient", Long.toUnsignedString(transaction.getRecipientId()));
        }
        json.put("amountATM", transaction.getAmountATM());
        json.put("feeATM", transaction.getFeeATM());

        if (StringUtils.isNotBlank(transaction.getReferencedTransactionFullHash())) {
            json.put("referencedTransactionFullHash", transaction.getReferencedTransactionFullHash());
        }
        json.put("ecBlockHeight", transaction.getECBlockHeight());
        json.put("ecBlockId", Long.toUnsignedString(transaction.getECBlockId()));
        Signature signature = transaction.getSignature();
        if (signature != null) {
            json.put("signature", Convert.toHexString(signature.bytes()));
        }
        JSONObject attachmentJSON = new JSONObject();
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            attachmentJSON.putAll(appendage.getJSONObject());
        }
        if (!attachmentJSON.isEmpty()) {
            json.put("attachment", attachmentJSON);
        }
    }

}
