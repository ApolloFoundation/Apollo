/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GenerateAccount extends APIServlet.APIRequestHandler {
    private static class GenerateAccountHolder {
        private static final GenerateAccount INSTANCE = new GenerateAccount();
    }

    public static GenerateAccount getInstance() {
        return GenerateAccountHolder.INSTANCE;
    }
    protected GenerateAccount() {
        super(new APITag[] {APITag.ACCOUNTS}, "passphrase");

    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = request.getParameter("passphrase");
        GeneratedAccount generatedAccount = Account.generateAccount(passphrase);
        if (generatedAccount != null) { return generatedAccount.toJSON();}
        return JSONResponses.ACCOUNT_GENERATION_ERROR;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
