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

import com.apollocurrency.aplwallet.apl.Exchange;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetExchanges extends APIServlet.APIRequestHandler {

    private static class GetExchangesHolder {
        private static final GetExchanges INSTANCE = new GetExchanges();
    }

    public static GetExchanges getInstance() {
        return GetExchangesHolder.INSTANCE;
    }

    private GetExchanges() {
        super(new APITag[] {APITag.MS}, "currency", "account", "firstIndex", "lastIndex", "timestamp", "includeCurrencyInfo");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        int timestamp = ParameterParser.getTimestamp(req);
        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        long accountId = ParameterParser.getAccountId(req, false);
        if (currencyId == 0 && accountId == 0) {
            return JSONResponses.MISSING_CURRENCY_ACCOUNT;
        }
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray exchangesData = new JSONArray();
        DbIterator<Exchange> exchanges = null;
        try {
            if (accountId == 0) {
                exchanges = Exchange.getCurrencyExchanges(currencyId, firstIndex, lastIndex);
            } else if (currencyId == 0) {
                exchanges = Exchange.getAccountExchanges(accountId, firstIndex, lastIndex);
            } else {
                exchanges = Exchange.getAccountCurrencyExchanges(accountId, currencyId, firstIndex, lastIndex);
            }
            while (exchanges.hasNext()) {
                Exchange exchange = exchanges.next();
                if (exchange.getTimestamp() < timestamp) {
                    break;
                }
                exchangesData.add(JSONData.exchange(exchange, includeCurrencyInfo));
            }
        } finally {
            DbUtils.close(exchanges);
        }
        response.put("exchanges", exchangesData);

        return response;
    }

    @Override
    protected boolean startDbTransaction() {
        return true;
    }

}
