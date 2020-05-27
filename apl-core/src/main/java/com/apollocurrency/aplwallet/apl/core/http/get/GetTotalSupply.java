/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Deprecated
@Vetoed
public class GetTotalSupply extends AbstractAPIRequestHandler {
    public GetTotalSupply() {
        super(new APITag[]{APITag.INFO, APITag.ACCOUNTS});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        JSONObject response = new JSONObject();
        response.put("totalAmount", lookupAccountService().getTotalSupply());
        return response;
    }
}
