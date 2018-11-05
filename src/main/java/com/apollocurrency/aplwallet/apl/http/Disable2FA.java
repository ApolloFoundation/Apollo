/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class Disable2FA extends APIServlet.APIRequestHandler {
    private Disable2FA() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "code2FA", "secretPhrase", "account", "passphrase");
    }

    private static class Disable2FAHolder {
        private static final Disable2FA INSTANCE = new Disable2FA();
    }

    public static Disable2FA getInstance() {
        return Disable2FAHolder.INSTANCE;
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        ParameterParser.TwoFactorAuthParameters params2FA = ParameterParser.parse2FARequest(request);
        int code = ParameterParser.getInt(request, "code2FA", Integer.MIN_VALUE, Integer.MAX_VALUE, true);

        TwoFactorAuthService.Status2FA status2FA;
        if (params2FA.isPassphrasePresent()) {
            status2FA = Account.disable2FA(params2FA.accountId, params2FA.passphrase, code);
        } else {
            status2FA = Account.disable2FA(params2FA.secretPhrase, code);
        }
        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", params2FA.accountId);
        response.put("status", status2FA);
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}

