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

package apl.http;

import apl.AplException;
import apl.Trade;
import apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllTrades extends APIServlet.APIRequestHandler {

    private static class GetAllTradesHolder {
        private static final GetAllTrades INSTANCE = new GetAllTrades();
    }

    public static GetAllTrades getInstance() {
        return GetAllTradesHolder.INSTANCE;
    }

    private GetAllTrades() {
        super(new APITag[] {APITag.AE}, "timestamp", "firstIndex", "lastIndex", "includeAssetInfo");
    }
    
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        final int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray trades = new JSONArray();
        try (DbIterator<Trade> tradeIterator = Trade.getAllTrades(firstIndex, lastIndex)) {
            while (tradeIterator.hasNext()) {
                Trade trade = tradeIterator.next();
                if (trade.getTimestamp() < timestamp) {
                    break;
                }
                trades.add(JSONData.trade(trade, includeAssetInfo));
            }
        }
        response.put("trades", trades);
        return response;
    }

}
