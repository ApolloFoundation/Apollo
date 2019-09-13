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
import com.apollocurrency.aplwallet.apl.util.AplException;
//import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStore;
import javax.enterprise.inject.Vetoed;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

@Vetoed
public class DeleteKey extends AbstractAPIRequestHandler {
   public DeleteKey() {
        super(new APITag[] {APITag.ACCOUNTS});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long accountId = ParameterParser.getAccountId(request, true);
        String passphrase = ParameterParser.getPassphrase(request, true);
        int code = ParameterParser.getInt(request, "code2FA", 0, Integer.MAX_VALUE, false);
        KeyStoreService.Status status = Helper2FA.deleteAccount(accountId, passphrase, code);
        JSONObject response = new JSONObject();
        response.put("status", status);
        JSONData.putAccount(response, "account", accountId);
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
