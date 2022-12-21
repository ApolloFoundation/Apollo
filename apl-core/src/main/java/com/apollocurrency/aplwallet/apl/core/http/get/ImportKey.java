/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

/**
 * Use com.apollocurrency.aplwallet.apl.core.rest.endpoint.KeyStoreController#importKeyStore
 */
@Deprecated
@Vetoed
public class ImportKey extends AbstractAPIRequestHandler {
    public ImportKey() {
        super(new APITag[]{APITag.ACCOUNT_CONTROL}, "secretBytes", "passphrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        Account2FAService account2FAService = CDI.current().select(Account2FAService.class).get();
        String passphrase = Convert.emptyToNull(HttpParameterParserUtil.getPassphrase(request, false));
        byte[] secretBytes = HttpParameterParserUtil.getBytes(request, "secretBytes", true);

        try {
            WalletKeysInfo walletKeysInfo = account2FAService.generateUserWallet(passphrase, secretBytes);
            return walletKeysInfo.toJSON();
        } catch (RestParameterException e) {
            return JSONResponses.vaultWalletError(0l, "import", e.getMessage());
        }
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
