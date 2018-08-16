/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
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
        return JSONData.getAccounts(firstIndex, lastIndex);
    }
}
