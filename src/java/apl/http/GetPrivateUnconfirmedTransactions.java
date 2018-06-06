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

import apl.Account;
import apl.Apl;
import apl.Transaction;
import apl.crypto.Crypto;
import apl.db.FilteringIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPrivateUnconfirmedTransactions extends APIServlet.APIRequestHandler {

    static final GetPrivateUnconfirmedTransactions instance = new GetPrivateUnconfirmedTransactions();

    private GetPrivateUnconfirmedTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS, APITag.ACCOUNTS}, "firstIndex", "lastIndex", "secretPhrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        Long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        JSONArray transactions = new JSONArray();
            try (FilteringIterator<? extends Transaction> transactionsIterator = new FilteringIterator<> (
                    Apl.getTransactionProcessor().getAllUnconfirmedTransactions(0, -1),
                    transaction -> accountId.equals(transaction.getSenderId()) || accountId.equals(transaction.getRecipientId()),
                    firstIndex, lastIndex)) {
                while (transactionsIterator.hasNext()) {
                    Transaction transaction = transactionsIterator.next();
                        transactions.add(JSONData.unconfirmedTransaction(transaction));
                }
            }
        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactions);
        return response;
    }

}

