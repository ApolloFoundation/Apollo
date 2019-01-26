/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetGenesisBalances extends AbstractAPIRequestHandler {
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
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        return JSONData.genesisBalancesJson(firstIndex, lastIndex);
    }
}
