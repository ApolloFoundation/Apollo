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

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Trade;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetTrades extends APIServlet.APIRequestHandler {

    private static class GetTradesHolder {
        private static final GetTrades INSTANCE = new GetTrades();
    }

    public static GetTrades getInstance() {
        return GetTradesHolder.INSTANCE;
    }

    private GetTrades() {
        super(new APITag[] {APITag.AE}, "asset", "account", "firstIndex", "lastIndex", "timestamp", "includeAssetInfo");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", false);
        long accountId = ParameterParser.getAccountId(req, false);
        if (assetId == 0 && accountId == 0) {
            return JSONResponses.MISSING_ASSET_ACCOUNT;
        }

        int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray tradesData = new JSONArray();
        DbIterator<Trade> trades = null;
        try {
            if (accountId == 0) {
                trades = Trade.getAssetTrades(assetId, firstIndex, lastIndex);
            } else if (assetId == 0) {
                trades = Trade.getAccountTrades(accountId, firstIndex, lastIndex);
            } else {
                trades = Trade.getAccountAssetTrades(accountId, assetId, firstIndex, lastIndex);
            }
            while (trades.hasNext()) {
                Trade trade = trades.next();
                if (trade.getTimestamp() < timestamp) {
                    break;
                }
                tradesData.add(JSONData.trade(trade, includeAssetInfo));
            }
        } finally {
            DbUtils.close(trades);
        }
        response.put("trades", tradesData);

        return response;
    }

    @Override
    protected boolean startDbTransaction() {
        return true;
    }

}
