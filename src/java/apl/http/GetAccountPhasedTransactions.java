/*
* Copyright © 2013-2016 The Nxt Core Developers.
* Copyright © 2016-2017 Jelurida IP B.V.
*
* See the LICENSE.txt file at the top-level directory of this distribution
* for licensing information.
*
* Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
* no part of the Nxt software, including this file, may be copied, modified,
* propagated, or distributed except according to the terms contained in the
* LICENSE.txt file.
*
* Removal or modification of this copyright notice is prohibited.
*
*/

/*
* Copyright © 2018 Apollo Foundation
*/

package apl.http;

import apl.AplException;
import apl.PhasingPoll;
import apl.Transaction;
import apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAccountPhasedTransactions extends APIServlet.APIRequestHandler {
    private static class GetAccountPhasedTransactionsHolder {
        private static final GetAccountPhasedTransactions INSTANCE = new GetAccountPhasedTransactions();
    }

    public static GetAccountPhasedTransactions getInstance() {
        return GetAccountPhasedTransactionsHolder.INSTANCE;
    }

    private GetAccountPhasedTransactions() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.PHASING},
                "account", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long accountId = ParameterParser.getAccountId(req, true);

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray transactions = new JSONArray();

        try (DbIterator<? extends Transaction> iterator =
                PhasingPoll.getAccountPhasedTransactions(accountId, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactions.add(JSONData.transaction(false, transaction));
            }
        }

        JSONObject response = new JSONObject();
        response.put("transactions", transactions);

        return response;
    }
}
