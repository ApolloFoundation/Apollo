/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
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
        String passphrase = Convert.emptyToNull(HttpParameterParserUtil.getPassphrase(request, false));
        byte[] secretBytes = HttpParameterParserUtil.getBytes(request, "secretBytes", true);

        try {
            WalletKeysInfo walletKeysInfo = Helper2FA.importSecretBytes(passphrase, secretBytes);
            return walletKeysInfo.toJSON();
        } catch (ParameterException e) {
            return JSONResponses.vaultWalletError(0l, "import", e.getMessage());
        }
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
