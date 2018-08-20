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

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchAccounts extends APIServlet.APIRequestHandler {

    private static class SearchAccountsHolder {
        private static final SearchAccounts INSTANCE = new SearchAccounts();
    }

    public static SearchAccounts getInstance() {
        return SearchAccountsHolder.INSTANCE;
    }

    private SearchAccounts() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.SEARCH}, "query", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        if (query.isEmpty()) {
            return JSONResponses.missing("query");
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        try (DbIterator<Account.AccountInfo> accounts = Account.searchAccounts(query, firstIndex, lastIndex)) {
            for (Account.AccountInfo account : accounts) {
                JSONObject accountJSON = new JSONObject();
                JSONData.putAccount(accountJSON, "account", account.getAccountId());
                if (account.getName() != null) {
                    accountJSON.put("name", account.getName());
                }
                if (account.getDescription() != null) {
                    accountJSON.put("description", account.getDescription());
                }
                accountsJSONArray.add(accountJSON);
            }
        }
        response.put("accounts", accountsJSONArray);
        return response;
    }

}
