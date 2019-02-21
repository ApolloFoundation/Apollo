/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetTotalSupply extends AbstractAPIRequestHandler {
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
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        JSONObject response = new JSONObject();
        response.put("totalAmount", Account.getTotalSupply());
        return response;
    }
}
