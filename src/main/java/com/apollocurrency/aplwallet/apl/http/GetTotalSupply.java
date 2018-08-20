/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetTotalSupply extends APIServlet.APIRequestHandler{
    private static class GetTotalAmountHolder {
        private static final GetTotalSupply INSTANCE = new GetTotalSupply();
    }

    public static GetTotalSupply getInstance() {
        return GetTotalAmountHolder.INSTANCE;
    }
    private GetTotalSupply() {
        super(new APITag[] {APITag.INFO, APITag.ACCOUNTS});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        JSONObject response = new JSONObject();
        response.put("totalAmount", Account.getTotalSupply());
        return response;
    }
}
