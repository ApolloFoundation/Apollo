/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

public class ExportKey extends AbstractAPIRequestHandler {
    private static class ExportPrivateKeyHolder {
        private static final ExportKey INSTANCE = new ExportKey();
    }

    public static ExportKey getInstance() {
        return ExportPrivateKeyHolder.INSTANCE;
    }
    private ExportKey() {
        super(new APITag[] {APITag.ACCOUNTS});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {

        String passphrase = ParameterParser.getPassphrase(request, true);
        long accountId = ParameterParser.getAccountId(request, true);

        File keyStoreFile = Helper2FA.getKeyStoreFile(accountId, passphrase);
        //TODO send file.

        //todo delete this mock
        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", accountId);
        response.put("secretBytes", Long.valueOf(123l).toString(16));
        //
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
