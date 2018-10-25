/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.KeyStore;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class DeleteKey extends APIServlet.APIRequestHandler {
    private static class DeleteKeyHolder {
        private static final DeleteKey INSTANCE = new DeleteKey();
    }

    public static DeleteKey getInstance() {
        return DeleteKeyHolder.INSTANCE;
    }
    private DeleteKey() {
        super(new APITag[] {APITag.ACCOUNTS});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long accountId = ParameterParser.getAccountId(request, true);
        String passphrase = ParameterParser.getPassphrase(request, true);
        int code = ParameterParser.getInt(request, "code2FA", 0, Integer.MAX_VALUE, false);
        KeyStore.Status status = Account.deleteAccount(accountId, passphrase, code);
        JSONObject response = new JSONObject();
        response.put("status", status);
        JSONData.putAccount(response, "account", accountId);
        return response;
    }

    @Override
    protected String accountName2FA() {
        return "account";
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
