/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Use com.apollocurrency.aplwallet.apl.core.rest.endpoint.KeyStoreController#importKeyStore
 */
@Deprecated
public class ImportKey extends AbstractAPIRequestHandler {
    private static class ImportKeyHolder {
        private static final ImportKey INSTANCE = new ImportKey();
    }

    public static ImportKey getInstance() {
        return ImportKeyHolder.INSTANCE;
    }
    private ImportKey() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL}, "secretBytes", "passphrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(request, false));
        byte[] secretBytes = ParameterParser.getBytes(request, "secretBytes", true);

        try {
            WalletKeysInfo walletKeysInfo = Helper2FA.importSecretBytes(passphrase, secretBytes);
            return walletKeysInfo.toJSON();
        } catch (ParameterException e){
            return JSONResponses.vaultWalletError(0l, "import", e.getMessage());
        }
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
