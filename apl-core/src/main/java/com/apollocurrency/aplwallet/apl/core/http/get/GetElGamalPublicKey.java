/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

//TODO: Refactor that according to Feng shui
@Vetoed
public final class GetElGamalPublicKey extends AbstractAPIRequestHandler {

    public GetElGamalPublicKey() {
        super(new APITag[]{APITag.TRANSACTIONS}, "publicKey");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        JSONObject response = new JSONObject();
        response.put("ElGamalX", elGamal.getServerElGamalPublicKey().getPrivateKeyX().toString(16));
        response.put("ElGamalY", elGamal.getServerElGamalPublicKey().getPrivateKeyY().toString(16));
        return response;
    }
}


