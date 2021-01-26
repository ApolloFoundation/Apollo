/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
@Deprecated
public class DeleteKey extends AbstractAPIRequestHandler {
    public DeleteKey() {
        super(new APITag[]{APITag.ACCOUNTS});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        Account2FAService account2FAService = CDI.current().select(Account2FAService.class).get();

        long accountId = HttpParameterParserUtil.getAccountId(request, true);
        String passphrase = HttpParameterParserUtil.getPassphrase(request, true);
        int code = HttpParameterParserUtil.getInt(request, "code2FA", 0, Integer.MAX_VALUE, false);
        KMSResponseStatus status = account2FAService.deleteAccount(accountId, passphrase, code);
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
