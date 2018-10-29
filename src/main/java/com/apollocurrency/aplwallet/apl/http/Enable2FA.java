/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthDetails;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class Enable2FA extends APIServlet.APIRequestHandler {
    private Enable2FA() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "passphrase", "account", "secretPhrase");
    }

    private static class Enable2FAHolder {
        private static final Enable2FA INSTANCE = new Enable2FA();
    }

    public static Enable2FA getInstance() {
        return Enable2FAHolder.INSTANCE;
    }
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        ParameterParser.TwoFactorAuthParameters params2FA = ParameterParser.parse2FARequest(request);

        TwoFactorAuthDetails twoFactorAuthDetails;
        if (params2FA.isPassphrasePresent()) {
            twoFactorAuthDetails = Account.enable2FA(params2FA.accountId, params2FA.passphrase);
        } else {
            twoFactorAuthDetails = Account.enable2FA(params2FA.secretPhrase);
        }
        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", params2FA.accountId);
        response.put("secret", twoFactorAuthDetails.getSecret());
        response.put("qrCodeUrl", twoFactorAuthDetails.getQrCodeUrl());
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
