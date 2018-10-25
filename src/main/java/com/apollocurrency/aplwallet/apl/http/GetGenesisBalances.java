/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetGenesisBalances extends APIServlet.APIRequestHandler {
    private static class GetGenesisBalancesHolder {
        private static final GetGenesisBalances INSTANCE = new GetGenesisBalances();
    }

    public static GetGenesisBalances getInstance() {
        return GetGenesisBalancesHolder.INSTANCE;
    }
    protected GetGenesisBalances() {
        super(new APITag[] {APITag.ACCOUNTS}, "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        return JSONData.genesisBalancesJson(firstIndex, lastIndex);
    }
}
