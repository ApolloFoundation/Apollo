/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import javax.servlet.http.HttpServletRequest;


import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.util.AplException;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

//TODO: Refactor that according to Feng shui
@Vetoed
public final class GetElGamalPublicKey extends AbstractAPIRequestHandler {

    public GetElGamalPublicKey() {
        super(new APITag[] {APITag.TRANSACTIONS}, "publicKey");
    }
    
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        
        JSONObject response = new JSONObject();
        response.put("ElGamalX", API.getServerElGamalPublicKey().getPrivateKeyX().toString(16));
        response.put("ElGamalY", API.getServerElGamalPublicKey().getPrivateKeyY().toString(16));
        return response;
    }
}


