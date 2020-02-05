/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParser;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

/**
 * New export will export keystore file instead secret.
 * Use com.apollocurrency.aplwallet.apl.core.rest.endpoint.KeyStoreController#importKeyStore(javax.servlet.http.HttpServletRequest)
 */
@Deprecated
@Vetoed
public class ExportKey extends AbstractAPIRequestHandler {

    public ExportKey() {
        super(new APITag[] {APITag.ACCOUNTS});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {

        String passphrase = HttpParameterParser.getPassphrase(request, true);
        long accountId = HttpParameterParser.getAccountId(request, true);

        byte [] secretBytes = Helper2FA.findAplSecretBytes(accountId, passphrase);

        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", accountId);
        response.put("secretBytes", Convert.toHexString(secretBytes) );

        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }

    @Override
    protected boolean is2FAProtected() {
        return super.is2FAProtected();
    }
}
