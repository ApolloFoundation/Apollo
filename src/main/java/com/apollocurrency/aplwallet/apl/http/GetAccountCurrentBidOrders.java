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

import com.apollocurrency.aplwallet.apl.Order;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountCurrentBidOrders extends APIServlet.APIRequestHandler {

    private static class GetAccountCurrentBidOrdersHolder {
        private static final GetAccountCurrentBidOrders INSTANCE = new GetAccountCurrentBidOrders();
    }

    public static GetAccountCurrentBidOrders getInstance() {
        return GetAccountCurrentBidOrdersHolder.INSTANCE;
    }

    private GetAccountCurrentBidOrders() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.AE}, "account", "asset", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long accountId = ParameterParser.getAccountId(req, true);
        long assetId = ParameterParser.getUnsignedLong(req, "asset", false);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        DbIterator<Order.Bid> bidOrders;
        if (assetId == 0) {
            bidOrders = Order.Bid.getBidOrdersByAccount(accountId, firstIndex, lastIndex);
        } else {
            bidOrders = Order.Bid.getBidOrdersByAccountAsset(accountId, assetId, firstIndex, lastIndex);
        }
        JSONArray orders = new JSONArray();
        try {
            while (bidOrders.hasNext()) {
                orders.add(JSONData.bidOrder(bidOrders.next()));
            }
        } finally {
            bidOrders.close();
        }
        JSONObject response = new JSONObject();
        response.put("bidOrders", orders);
        return response;
    }

}
