/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.KeyStore;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class ImportKey extends APIServlet.APIRequestHandler {
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
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(request, false));
        byte[] secretBytes = ParameterParser.getBytes(request, "secretBytes", true);

        Pair<KeyStore.Status, String> statusPassphrasePair = Account.importSecretBytes(passphrase, secretBytes);
        JSONObject response = new JSONObject();
        response.put("status", statusPassphrasePair.getLeft());
        response.put("passphrase", statusPassphrasePair.getRight());
        JSONData.putAccount(response, "account", Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes))));
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }
}
