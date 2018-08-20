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

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Trade;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetOrderTrades extends APIServlet.APIRequestHandler {

    private static class GetOrderTradesHolder {
        private static final GetOrderTrades INSTANCE = new GetOrderTrades();
    }

    public static GetOrderTrades getInstance() {
        return GetOrderTradesHolder.INSTANCE;
    }

    private GetOrderTrades() {
        super(new APITag[] {APITag.AE}, "askOrder", "bidOrder", "includeAssetInfo", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long askOrderId = ParameterParser.getUnsignedLong(req, "askOrder", false);
        long bidOrderId = ParameterParser.getUnsignedLong(req, "bidOrder", false);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        if (askOrderId == 0 && bidOrderId == 0) {
            return JSONResponses.missing("askOrder", "bidOrder");
        }

        JSONObject response = new JSONObject();
        JSONArray tradesData = new JSONArray();
        if (askOrderId != 0 && bidOrderId != 0) {
            Trade trade = Trade.getTrade(askOrderId, bidOrderId);
            if (trade != null) {
                tradesData.add(JSONData.trade(trade, includeAssetInfo));
            }
        } else {
            DbIterator<Trade> trades = null;
            try {
                if (askOrderId != 0) {
                    trades = Trade.getAskOrderTrades(askOrderId, firstIndex, lastIndex);
                } else {
                    trades = Trade.getBidOrderTrades(bidOrderId, firstIndex, lastIndex);
                }
                while (trades.hasNext()) {
                    Trade trade = trades.next();
                    tradesData.add(JSONData.trade(trade, includeAssetInfo));
                }
            } finally {
                DbUtils.close(trades);
            }
        }
        response.put("trades", tradesData);

        return response;
    }

}
