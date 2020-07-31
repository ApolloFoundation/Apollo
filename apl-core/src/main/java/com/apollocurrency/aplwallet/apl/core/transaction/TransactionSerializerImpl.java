/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionSerializerImpl implements TransactionSerializer {
    private final PrunableLoadingService prunableService;

    @Inject
    public TransactionSerializerImpl(PrunableLoadingService prunableService) {
        this.prunableService = prunableService;
    }

    @Override
    public JSONObject toJson(Transaction transaction) {
        JSONObject json = new JSONObject();
        json.put("id", Long.toUnsignedString(transaction.getId()));
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
        json.put("signature", Convert.toHexString(transaction.getSignature()));
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
