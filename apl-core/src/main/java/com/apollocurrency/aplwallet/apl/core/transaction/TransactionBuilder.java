/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import org.json.simple.JSONObject;

public class TransactionBuilder {

    static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountATM, long feeATM, short deadline, Attachment attachment, int timestamp) {
        return new TransactionImpl.BuilderImpl((byte) 1, senderPublicKey, amountATM, feeATM, deadline, (AbstractAttachment) attachment, timestamp);
    }

    static Transaction.Builder newTransactionBuilder(byte[] transactionBytes) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes);
    }

    static Transaction.Builder newTransactionBuilder(JSONObject transactionJSON) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionJSON);
    }

    static Transaction.Builder newTransactionBuilder(byte[] transactionBytes, JSONObject prunableAttachments) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes, prunableAttachments);
    }
}
