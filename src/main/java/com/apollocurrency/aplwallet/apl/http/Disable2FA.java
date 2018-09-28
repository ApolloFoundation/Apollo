/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class Disable2FA extends APIServlet.APIRequestHandler {
    private Disable2FA() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "passphrase", "account", "code");
    }

    private static class Disable2FAHolder {
        private static final Disable2FA INSTANCE = new Disable2FA();
    }

    public static Disable2FA getInstance() {
        return Disable2FAHolder.INSTANCE;
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = ParameterParser.getPassphrase(request, true);
        long accountId = ParameterParser.getAccountId(request, true);
        int code = ParameterParser.getInt(request, "code", Integer.MIN_VALUE, Integer.MAX_VALUE, true);
        JSONObject response = new JSONObject();
        Account.disable2FA(accountId, passphrase, code);
        JSONData.putAccount(response, "account", accountId);
        response.put("2FAdisabled", true);
        return response;
    }
}

