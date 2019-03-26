/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.util.AplException;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

@Vetoed
public class Disable2FA extends AbstractAPIRequestHandler {
    public Disable2FA() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "secretPhrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        TwoFactorAuthParameters params2FA = ParameterParser.parse2FARequest(request);
        int code = ParameterParser.getInt(request, "code2FA", Integer.MIN_VALUE, Integer.MAX_VALUE, true);

        Status2FA status2FA;
        if (params2FA.isPassphrasePresent()) {
            status2FA = Helper2FA.disable2FA(params2FA.getAccountId(), params2FA.getPassphrase(), code);
        } else {
            status2FA = Helper2FA.disable2FA(params2FA.getSecretPhrase(), code);
        }
        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", params2FA.getAccountId());
        response.put("status", status2FA);
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

