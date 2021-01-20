/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.vault.service.KMSv1;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

/**
 * New export will export keystore file instead secret.
 * Use com.apollocurrency.aplwallet.apl.core.rest.endpoint.KeyStoreController#importKeyStore(javax.servlet.http.HttpServletRequest)
 */
@Deprecated
@Vetoed
public class ExportKey extends AbstractAPIRequestHandler {

    public ExportKey() {
        super(new APITag[]{APITag.ACCOUNTS});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        KMSv1 kmSv1 = CDI.current().select(KMSv1.class).get();
        String passphrase = HttpParameterParserUtil.getPassphrase(request, true);
        long accountId = HttpParameterParserUtil.getAccountId(request, true);

        byte[] secretBytes = kmSv1.getAplSecretBytes(accountId, passphrase);

        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", accountId);
        response.put("secretBytes", Convert.toHexString(secretBytes));

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
