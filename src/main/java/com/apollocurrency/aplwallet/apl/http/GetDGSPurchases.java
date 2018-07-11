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

import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSPurchases extends APIServlet.APIRequestHandler {

    private static class GetDGSPurchasesHolder {
        private static final GetDGSPurchases INSTANCE = new GetDGSPurchases();
    }

    public static GetDGSPurchases getInstance() {
        return GetDGSPurchasesHolder.INSTANCE;
    }

    private GetDGSPurchases() {
        super(new APITag[] {APITag.DGS}, "seller", "buyer", "firstIndex", "lastIndex", "withPublicFeedbacksOnly", "completed");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long sellerId = ParameterParser.getAccountId(req, "seller", false);
        long buyerId = ParameterParser.getAccountId(req, "buyer", false);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final boolean completed = "true".equalsIgnoreCase(req.getParameter("completed"));
        final boolean withPublicFeedbacksOnly = "true".equalsIgnoreCase(req.getParameter("withPublicFeedbacksOnly"));


        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        response.put("purchases", purchasesJSON);

        DbIterator<DigitalGoodsStore.Purchase> purchases;
        if (sellerId == 0 && buyerId == 0) {
            purchases = DigitalGoodsStore.Purchase.getPurchases(withPublicFeedbacksOnly, completed, firstIndex, lastIndex);
        } else if (sellerId != 0 && buyerId == 0) {
            purchases = DigitalGoodsStore.Purchase.getSellerPurchases(sellerId, withPublicFeedbacksOnly, completed, firstIndex, lastIndex);
        } else if (sellerId == 0) {
            purchases = DigitalGoodsStore.Purchase.getBuyerPurchases(buyerId, withPublicFeedbacksOnly, completed, firstIndex, lastIndex);
        } else {
            purchases = DigitalGoodsStore.Purchase.getSellerBuyerPurchases(sellerId, buyerId, withPublicFeedbacksOnly, completed, firstIndex, lastIndex);
        }
        try {
            while (purchases.hasNext()) {
                purchasesJSON.add(JSONData.purchase(purchases.next()));
            }
        } finally {
            DbUtils.close(purchases);
        }
        return response;
    }

}
