/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAccounts extends APIServlet.APIRequestHandler {
    private static class GetAccountsHolder {
        private static final GetAccounts INSTANCE = new GetAccounts();
    }

    public static GetAccounts getInstance() {
        return GetAccountsHolder.INSTANCE;
    }
    private GetAccounts() {
        super(new APITag[] {APITag.ACCOUNTS}, "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        int firstIndex = ParameterParser.getFirstIndex(request);
        int lastIndex = ParameterParser.getLastIndex(request);

        JSONArray accounts = new JSONArray();
        DbIterator<Account> iterator = Account.getAccounts(firstIndex, lastIndex);
        while (iterator.hasNext()) {
            Account account = iterator.next();
            JSONObject json = JSONData.accountBalance(account, false);
            JSONData.putAccount(json, "account", account.getId());
            accounts.add(json);
        }
        JSONObject response = new JSONObject();
        response.put("accounts", accounts);
        return response;
    }
}
