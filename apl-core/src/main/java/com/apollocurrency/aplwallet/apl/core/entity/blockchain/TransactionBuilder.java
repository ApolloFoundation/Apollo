/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.util.annotation.ParentChildSpecific;
import com.apollocurrency.aplwallet.apl.util.annotation.ParentMarker;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionBuilder {
    public static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountATM, long feeATM, short deadline, Attachment attachment, int timestamp) {
        return new TransactionImpl.BuilderImpl((byte) 1, senderPublicKey, amountATM, feeATM, deadline, (AbstractAttachment) attachment, timestamp);
    }

    public static Transaction.Builder newTransactionBuilder(int version, byte[] senderPublicKey, long amountATM, long feeATM, short deadline, Attachment attachment, int timestamp) {
        return new TransactionImpl.BuilderImpl((byte) version, senderPublicKey, amountATM, feeATM, deadline, (AbstractAttachment) attachment, timestamp);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes);
    }

    public static Transaction.Builder newTransactionBuilder(JSONObject transactionJSON) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionJSON);
    }

    public static Transaction parseTransaction(JSONObject transactionJSON) throws AplException.NotValidException {
        Transaction transaction = newTransactionBuilder(transactionJSON).build();
        //TODO verify signature
/*
        if (transaction.getSignature() != null && !transaction.checkSignature()) {
            throw new AplException.NotValidException("Invalid transaction signature for transaction " + transaction.getJSONObject().toJSONString());
        }
*/

        return transaction;

    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes, JSONObject prunableAttachments) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes, prunableAttachments);
    }

    public static TransactionImpl parseTransaction2(JSONObject transactionData) throws AplException.NotValidException {
        TransactionImpl transaction = (TransactionImpl) newTransactionBuilder(transactionData).build();
        if (transaction.getSignature() != null) {
            @ParentChildSpecific(ParentMarker.MULTI_SIGNATURE)
            boolean rc = false;//transaction.checkSignature(new byte[][]{ transaction.senderPublicKey });
            if (!rc) {
                throw new AplException.NotValidException("Invalid transaction signature for transaction " + transaction.getJSONObject().toJSONString());
            }
        }
        return transaction;
    }
}
