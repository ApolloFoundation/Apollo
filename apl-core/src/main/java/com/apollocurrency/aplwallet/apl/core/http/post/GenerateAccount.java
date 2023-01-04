/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;

@Deprecated
@Vetoed
public class GenerateAccount extends AbstractAPIRequestHandler {
    public GenerateAccount() {
        super(new APITag[]{APITag.ACCOUNTS}, "passphrase");

    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = request.getParameter("passphrase");
        Account2FAService account2FAService = CDI.current().select(Account2FAService.class).get();
        WalletKeysInfo aplWalletKey = account2FAService.generateUserWallet(passphrase);
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
