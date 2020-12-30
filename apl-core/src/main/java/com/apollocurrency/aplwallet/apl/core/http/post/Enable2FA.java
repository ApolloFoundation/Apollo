/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.vault.model.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.vault.model.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.vault.service.Account2FAService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
@Deprecated
public class Enable2FA extends AbstractAPIRequestHandler {
    public Enable2FA() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.TWO_FACTOR_AUTH}, "secretPhrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        TwoFactorAuthParameters params2FA = HttpParameterParserUtil.parse2FARequest(request);
        Account2FAService account2FAService = CDI.current().select(Account2FAService.class).get();

        TwoFactorAuthDetails twoFactorAuthDetails = account2FAService.enable2FA(params2FA);


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
