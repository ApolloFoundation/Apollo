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

import com.apollocurrency.aplwallet.apl.Order;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllOpenBidOrders extends APIServlet.APIRequestHandler {

    private static class GetAllOpenBidOrdersHolder {
        private static final GetAllOpenBidOrders INSTANCE = new GetAllOpenBidOrders();
    }

    public static GetAllOpenBidOrders getInstance() {
        return GetAllOpenBidOrdersHolder.INSTANCE;
    }

    private GetAllOpenBidOrders() {
        super(new APITag[] {APITag.AE}, "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        JSONArray ordersData = new JSONArray();

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        try (DbIterator<Order.Bid> bidOrders = Order.Bid.getAll(firstIndex, lastIndex)) {
            while (bidOrders.hasNext()) {
                ordersData.add(JSONData.bidOrder(bidOrders.next()));
            }
        }

        response.put("openOrders", ordersData);
        return response;
    }

}
