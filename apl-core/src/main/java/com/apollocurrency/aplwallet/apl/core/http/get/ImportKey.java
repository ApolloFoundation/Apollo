/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStore;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.model.WalletsInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

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

        WalletsInfo walletsInfo = Helper2FA.importSecretBytes(passphrase, secretBytes);
        JSONObject response = new JSONObject();
        response.put("status", walletsInfo != null ? VaultKeyStore.Status.OK : VaultKeyStore.Status.BAD_CREDENTIALS);
        response.put("passphrase", walletsInfo.getPassphrase());
        JSONData.putAccount(response, "account", walletsInfo.getAplId());
        response.put("eth", walletsInfo.getEthAddress());
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
