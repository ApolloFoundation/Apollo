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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountCurrencies extends APIServlet.APIRequestHandler {

    private static class GetAccountCurrenciesHolder {
        private static final GetAccountCurrencies INSTANCE = new GetAccountCurrencies();
    }

    public static GetAccountCurrencies getInstance() {
        return GetAccountCurrenciesHolder.INSTANCE;
    }

    private GetAccountCurrencies() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.MS}, "account", "currency", "height", "includeCurrencyInfo");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long accountId = ParameterParser.getAccountId(req, true);
        int height = ParameterParser.getHeight(req);
        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        if (currencyId == 0) {
            JSONObject response = new JSONObject();
            try (DbIterator<Account.AccountCurrency> accountCurrencies = Account.getAccountCurrencies(accountId, height, 0, -1)) {
                JSONArray currencyJSON = new JSONArray();
                while (accountCurrencies.hasNext()) {
                    currencyJSON.add(JSONData.accountCurrency(accountCurrencies.next(), false, includeCurrencyInfo));
                }
                response.put("accountCurrencies", currencyJSON);
                return response;
            }
        } else {
            Account.AccountCurrency accountCurrency = Account.getAccountCurrency(accountId, currencyId, height);
            if (accountCurrency != null) {
                return JSONData.accountCurrency(accountCurrency, false, includeCurrencyInfo);
            }
            return JSON.emptyJSON;
        }
    }

}
