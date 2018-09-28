/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthDetails;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class Enable2FA extends APIServlet.APIRequestHandler {
    private Enable2FA() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "passphrase", "account");
    }

    private static class Enable2FAHolder {
        private static final Enable2FA INSTANCE = new Enable2FA();
    }

    public static Enable2FA getInstance() {
        return Enable2FAHolder.INSTANCE;
    }
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = ParameterParser.getPassphrase(request, true);
        long accountId = ParameterParser.getAccountId(request, true);
        JSONObject response = new JSONObject();
        TwoFactorAuthDetails twoFactorAuthDetails = Account.enable2FA(accountId, passphrase);
        JSONData.putAccount(response, "account", accountId);
        response.put("secret", twoFactorAuthDetails.getSecret());
        response.put("qrCodeUrl", twoFactorAuthDetails.getQrCodeUrl());
        return response;
    }
}
