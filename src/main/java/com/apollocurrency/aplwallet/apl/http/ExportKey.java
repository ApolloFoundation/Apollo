/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class ExportKey extends APIServlet.APIRequestHandler {
    private static class ExportPrivateKeyHolder {
        private static final ExportKey INSTANCE = new ExportKey();
    }

    public static ExportKey getInstance() {
        return ExportPrivateKeyHolder.INSTANCE;
    }
    private ExportKey() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL}, "account", "passphrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = ParameterParser.getPassphrase(request, true);
        long accountId = ParameterParser.getAccountId(request, true);
        byte[] keySeed = Account.exportKeySeed(passphrase, accountId);
        JSONObject response = new JSONObject();
        response.put("account", Convert.rsAccount(accountId));
        response.put("keySeed", Convert.toHexString(keySeed));
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

}
