/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_TRANSACTION;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPrivateTransaction extends APIServlet.APIRequestHandler {

    private static class GetPrivateTransactionHolder {
        private static final GetPrivateTransaction INSTANCE = new GetPrivateTransaction();
    }

    public static GetPrivateTransaction getInstance() {
        return GetPrivateTransactionHolder.INSTANCE;
    }

    private GetPrivateTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transaction", "fullHash", "secretPhrase", "publicKey", "account", "passphrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        ParameterParser.PrivateTransactionsAPIData data = ParameterParser.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }
        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        String transactionFullHash = Convert.emptyToNull(req.getParameter("fullHash"));
        if (transactionIdString == null && transactionFullHash == null) {
            return MISSING_TRANSACTION;
        }
        long accountId = data.getAccountId();
        long transactionId = 0;
        Transaction transaction;
        try {
            if (transactionIdString != null) {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Apl.getBlockchain().getTransaction(transactionId);
            } else {
                transaction = Apl.getBlockchain().getTransactionByFullHash(transactionFullHash);
                if (transaction == null) {
                    return UNKNOWN_TRANSACTION;
                }
            }
        }
        catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }
        JSONObject response;
        if (transaction == null) {
            transaction = Apl.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transaction == null || !transaction.getType().equals(TransactionType.Payment.PRIVATE) || transaction.getType().equals(TransactionType.Payment.PRIVATE) && (transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId )) {
                return UNKNOWN_TRANSACTION;
            }
            if (data.isEncrypt()) {
                response = JSONData.encryptedUnconfirmedTransaction(transaction, data.getSharedKey());
            } else {
                response = JSONData.unconfirmedTransaction(transaction);
            }
        } else {
            if (!transaction.getType().equals(TransactionType.Payment.PRIVATE) || transaction.getType().equals(TransactionType.Payment.PRIVATE) && (transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId )) {
                return UNKNOWN_TRANSACTION;
            }
            if (data.isEncrypt()) {
                response = JSONData.encryptedTransaction(transaction, data.getSharedKey());
            } else {
                response = JSONData.transaction(transaction, false, false);
            }
        }
        response.put("serverPublicKey", Convert.toHexString(API.getServerPublicKey()));
        return response;
    }
}

