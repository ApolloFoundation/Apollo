/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

@Vetoed
public class Enable2FA extends AbstractAPIRequestHandler {
    public Enable2FA() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "secretPhrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        TwoFactorAuthParameters params2FA = ParameterParser.parse2FARequest(request);

        TwoFactorAuthDetails twoFactorAuthDetails;
        if (params2FA.isPassphrasePresent()) {
            twoFactorAuthDetails = Helper2FA.enable2FA(params2FA.getAccountId(), params2FA.getPassphrase());
        } else {
            twoFactorAuthDetails = Helper2FA.enable2FA(params2FA.getSecretPhrase());
        }
        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", params2FA.getAccountId());
        response.put("secret", twoFactorAuthDetails.getSecret());
        response.put("qrCodeUrl", twoFactorAuthDetails.getQrCodeUrl());
        return response;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
