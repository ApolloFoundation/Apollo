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

import com.apollocurrency.aplwallet.apl.Exchange;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetLastExchanges extends APIServlet.APIRequestHandler {

    private static class GetLastExchangesHolder {
        private static final GetLastExchanges INSTANCE = new GetLastExchanges();
    }

    public static GetLastExchanges getInstance() {
        return GetLastExchangesHolder.INSTANCE;
    }

    private GetLastExchanges() {
        super(new APITag[] {APITag.MS}, "currencies", "currencies", "currencies"); // limit to 3 for testing
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long[] currencyIds = ParameterParser.getUnsignedLongs(req, "currencies");
        JSONArray exchangesJSON = new JSONArray();
        List<Exchange> exchanges = Exchange.getLastExchanges(currencyIds);
        exchanges.forEach(exchange -> exchangesJSON.add(JSONData.exchange(exchange, false)));
        JSONObject response = new JSONObject();
        response.put("exchanges", exchangesJSON);
        return response;
    }

}
