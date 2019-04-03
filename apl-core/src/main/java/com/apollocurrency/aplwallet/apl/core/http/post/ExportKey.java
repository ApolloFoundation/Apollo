/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

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
        byte[] secretBytes = Helper2FA.exportSecretBytes(passphrase, accountId);
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
