/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Currency;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyIds extends APIServlet.APIRequestHandler {

    private static class GetCurrencyIdsHolder {
        private static final GetCurrencyIds INSTANCE = new GetCurrencyIds();
    }

    public static GetCurrencyIds getInstance() {
        return GetCurrencyIdsHolder.INSTANCE;
    }

    private GetCurrencyIds() {
        super(new APITag[] {APITag.MS}, "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray currencyIds = new JSONArray();
        try (DbIterator<Currency> currencies = Currency.getAllCurrencies(firstIndex, lastIndex)) {
            for (Currency currency : currencies) {
                currencyIds.add(Long.toUnsignedString(currency.getId()));
            }
        }
        JSONObject response = new JSONObject();
        response.put("currencyIds", currencyIds);
        return response;
    }

}
