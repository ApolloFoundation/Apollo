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

public class Confirm2FA extends APIServlet.APIRequestHandler {
    private Confirm2FA() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "secretPhrase");
    }

    private static class Confirm2FAHolder {
        private static final Confirm2FA INSTANCE = new Confirm2FA();
    }

    public static Confirm2FA getInstance() {
        return Confirm2FA.Confirm2FAHolder.INSTANCE;
    }
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        ParameterParser.TwoFactorAuthParameters params2FA = ParameterParser.parse2FARequest(request);
        int code = ParameterParser.getInt(request, "code2FA", Integer.MIN_VALUE, Integer.MAX_VALUE, true);

        JSONObject response = new JSONObject();

        TwoFactorAuthService.Status2FA confirmStatus;
        if (params2FA.isPassphrasePresent()) {
            confirmStatus = Account.confirm2FA(params2FA.accountId, params2FA.passphrase, code);
        } else {
            confirmStatus = Account.confirm2FA(params2FA.secretPhrase, code);
        }

        JSONData.putAccount(response, "account", params2FA.accountId);
        response.put("status", confirmStatus);
        return response;
    }


    @Override
    protected String vaultAccountName() {
        return "account";
    }

    @Override
    protected boolean is2FAProtected() {
        return true;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
