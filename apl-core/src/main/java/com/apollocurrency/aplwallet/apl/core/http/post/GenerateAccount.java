/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
@Deprecated
@Vetoed
public class GenerateAccount extends AbstractAPIRequestHandler {
    public GenerateAccount() {
        super(new APITag[] {APITag.ACCOUNTS}, "passphrase");

    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = request.getParameter("passphrase");
        WalletKeysInfo aplWalletKey = Helper2FA.generateUserWallet(passphrase);
        if (aplWalletKey != null) {
            aplWalletKey.getAplWalletKey().setPassphrase(passphrase);
            return aplWalletKey.toJSON();
        }
        return JSONResponses.ACCOUNT_GENERATION_ERROR;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
