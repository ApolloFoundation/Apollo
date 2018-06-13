/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.http;

import apl.Apl;
import apl.AplException;
import apl.Transaction;
import apl.TransactionType;
import apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static apl.http.JSONResponses.*;

public final class GetPrivateTransaction extends APIServlet.APIRequestHandler {

    static final GetPrivateTransaction instance = new GetPrivateTransaction();

    private GetPrivateTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transaction", "fullHash", "secretPhrase", "publicKey");
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

