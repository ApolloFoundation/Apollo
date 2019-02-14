/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.math.BigInteger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetElGamalPublicKey extends AbstractAPIRequestHandler {
    
    private static class GetElGamalPublicKeyHolder {
        private static final GetElGamalPublicKey INSTANCE = new GetElGamalPublicKey();
    }
    
    public static GetElGamalPublicKey getInstance() {
        return GetElGamalPublicKeyHolder.INSTANCE;
    }

    private GetElGamalPublicKey() {
        super(new APITag[] {APITag.TRANSACTIONS}, "publicKey");
    }
    
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        
        JSONObject response = new JSONObject();
        response.put("ElGamalPrivate", API.getServerElGamalPublicKey().getPrivateKey().toString(16));
        response.put("ElGamalX", API.getServerElGamalPublicKey().getPrivateKeyX().toString(16));
        response.put("ElGamalY", API.getServerElGamalPublicKey().getPrivateKeyY().toString(16));
        return response;
    }
}


