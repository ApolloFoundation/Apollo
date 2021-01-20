/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.api.dto.auth.Status2FA;
import com.apollocurrency.aplwallet.api.dto.auth.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
@Deprecated
public class Disable2FA extends AbstractAPIRequestHandler {
    public Disable2FA() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "secretPhrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        Account2FAService account2FAService = CDI.current().select(Account2FAService.class).get();

        TwoFactorAuthParameters params2FA = HttpParameterParserUtil.parse2FARequest(request);
        int code = HttpParameterParserUtil.getInt(request, "code2FA", Integer.MIN_VALUE, Integer.MAX_VALUE, true);
        params2FA.setCode2FA(code);

        Status2FA status2FA = account2FAService.disable2FA(params2FA);

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

