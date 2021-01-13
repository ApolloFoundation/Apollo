/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Deprecated
@Vetoed
public class GetAccounts extends AbstractAPIRequestHandler {
    public GetAccounts() {
        super(new APITag[]{APITag.INFO}, "numberOfAccounts");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        int numberOfAccounts = HttpParameterParserUtil.getInt(request, "numberOfAccounts", Constants.MIN_TOP_ACCOUNTS_NUMBER,
            Constants.MAX_TOP_ACCOUNTS_NUMBER, false);
        return JSONData.getAccountsStatistic(Math.max(numberOfAccounts, Constants.MIN_TOP_ACCOUNTS_NUMBER));
    }
}
