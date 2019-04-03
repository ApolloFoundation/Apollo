/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.account.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GenerateAccount extends AbstractAPIRequestHandler {
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
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = request.getParameter("passphrase");
        GeneratedAccount generatedAccount = Helper2FA.generateAccount(passphrase);
        if (generatedAccount != null) { return generatedAccount.toJSON();}
        return JSONResponses.ACCOUNT_GENERATION_ERROR;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
