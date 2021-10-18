/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.io.JsonBuffer;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionJsonSerializerImpl implements TransactionJsonSerializer {
    private final PrunableLoadingService prunableService;
    private final TxBContext txBContext;

    @Inject
    public TransactionJsonSerializerImpl(PrunableLoadingService prunableService, BlockchainConfig blockchainConfig) {
        this.prunableService = prunableService;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    @Override
    public byte[] serialize(Transaction transaction) {
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(transaction, byteArrayTx);
        return byteArrayTx.array();
    }

    @Override
    public byte[] serializeUnsigned(Transaction transaction) {
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), byteArrayTx);
        return byteArrayTx.array();
    }

    @Override
    public JSONObject toJson(Transaction transaction) {
        //load not expired prunable attachments
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            prunableService.loadPrunable(transaction, appendage, false);
        }
        JsonBuffer buffer = new JsonBuffer();
        Result byteArrayTx = PayloadResult.createJsonResult(buffer);
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, byteArrayTx);
        return buffer.getJsonObject();
    }

    @Override
    public JSONObject toLegacyJsonFormat(Transaction transaction) {
        JSONObject json = new JSONObject();
        json.put("id", Long.toUnsignedString(transaction.getId()));
        TransactionType type = transaction.getType();
        TransactionTypes.TransactionTypeSpec spec = type.getSpec();
        json.put("errorMessage", null);
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
            prunableService.loadPrunable(transaction, appendage, false);
            attachmentJSON.putAll(appendage.getJSONObject());
        }
        if (!attachmentJSON.isEmpty()) {
            json.put("attachment", attachmentJSON);
        }
        json.put("version", transaction.getVersion());
        return json;
    }

    @Override
    public JSONObject getPrunableAttachmentJSON(Transaction transaction) {
        JSONObject prunableJSON = null;
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            prunableService.loadPrunable(transaction, appendage, false);
            if (appendage instanceof Prunable) {
                if (prunableJSON == null) {
                    prunableJSON = appendage.getJSONObject();
                } else {
                    prunableJSON.putAll(appendage.getJSONObject());
                }
            }
        }
        return prunableJSON;
    }
}
