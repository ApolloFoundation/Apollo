/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.json.simple.JSONObject;

import javax.inject.Inject;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class LegacyJsonSerializer {
    private final PrunableLoadingService prunableService;

    @Inject
    public LegacyJsonSerializer(PrunableLoadingService prunableService) {
        this.prunableService = prunableService;
    }

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

}
