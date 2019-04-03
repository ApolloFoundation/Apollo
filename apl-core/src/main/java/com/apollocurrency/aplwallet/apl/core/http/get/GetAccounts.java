/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAccounts extends AbstractAPIRequestHandler {
    private static class GetAccountsHolder {
        private static final GetAccounts INSTANCE = new GetAccounts();
    }

    public static GetAccounts getInstance() {
        return GetAccountsHolder.INSTANCE;
    }
    private GetAccounts() {
        super(new APITag[] {APITag.INFO}, "numberOfAccounts");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        int numberOfAccounts = ParameterParser.getInt(request, "numberOfAccounts", Constants.MIN_TOP_ACCOUNTS_NUMBER,
                Constants.MAX_TOP_ACCOUNTS_NUMBER, false);
        return JSONData.getAccountsStatistic(Math.max(numberOfAccounts, Constants.MIN_TOP_ACCOUNTS_NUMBER));
    }
}
