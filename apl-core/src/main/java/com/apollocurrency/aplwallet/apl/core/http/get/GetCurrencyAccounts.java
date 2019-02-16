/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyAccounts extends AbstractAPIRequestHandler {

    private static class GetCurrencyAccountsHolder {
        private static final GetCurrencyAccounts INSTANCE = new GetCurrencyAccounts();
    }

    public static GetCurrencyAccounts getInstance() {
        return GetCurrencyAccountsHolder.INSTANCE;
    }

    private GetCurrencyAccounts() {
        super(new APITag[] {APITag.MS}, "currency", "height", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long currencyId = ParameterParser.getUnsignedLong(req, "currency", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        int height = ParameterParser.getHeight(req);

        JSONArray accountCurrencies = new JSONArray();
        try (DbIterator<AccountCurrency> iterator = AccountCurrencyTable.getCurrencyAccounts(currencyId, height, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                AccountCurrency accountCurrency = iterator.next();
                accountCurrencies.add(JSONData.accountCurrency(accountCurrency, true, false));
            }
        }

        JSONObject response = new JSONObject();
        response.put("accountCurrencies", accountCurrencies);
        return response;

    }

}
