/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
@Deprecated
public class DeleteKey extends AbstractAPIRequestHandler {
    public DeleteKey() {
        super(new APITag[]{APITag.ACCOUNTS});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long accountId = HttpParameterParserUtil.getAccountId(request, true);
        String passphrase = HttpParameterParserUtil.getPassphrase(request, true);
        int code = HttpParameterParserUtil.getInt(request, "code2FA", 0, Integer.MAX_VALUE, false);
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
