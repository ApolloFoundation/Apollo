/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation B.V.,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.http;

import apl.Apl;
import apl.Transaction;
import apl.TransactionType;
import apl.db.FilteringIterator;
import apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static apl.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;

public final class GetPrivateUnconfirmedTransactions extends APIServlet.APIRequestHandler {

    static final GetPrivateUnconfirmedTransactions instance = new GetPrivateUnconfirmedTransactions();

    private GetPrivateUnconfirmedTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS, APITag.ACCOUNTS}, "firstIndex", "lastIndex", "secretPhrase", "publicKey");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        ParameterParser.PrivateTransactionsAPIData data = ParameterParser.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        JSONArray transactions = new JSONArray();
        try (FilteringIterator<? extends Transaction> transactionsIterator = new FilteringIterator<>(
                Apl.getTransactionProcessor().getAllUnconfirmedTransactions(0, -1),
                transaction -> data.getAccountId() == transaction.getSenderId() || data.getAccountId() == transaction.getRecipientId(),
                firstIndex, lastIndex)) {
            while (transactionsIterator.hasNext()) {
                Transaction transaction = transactionsIterator.next();
                if (data.isEncrypt() && transaction.getType() == TransactionType.Payment.PRIVATE) {
                    transactions.add(JSONData.encryptedUnconfirmedTransaction(transaction, data.getSharedKey()));
                } else {
                    transactions.add(JSONData.unconfirmedTransaction(transaction));
                }
            }
        }
        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactions);
        response.put("serverPublicKey", Convert.toHexString(API.getServerPublicKey()));
        return response;
    }

}

