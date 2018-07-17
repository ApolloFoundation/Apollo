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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSExpiredPurchases extends APIServlet.APIRequestHandler {

    private static class GetDGSExpiredPurchasesHolder {
        private static final GetDGSExpiredPurchases INSTANCE = new GetDGSExpiredPurchases();
    }

    public static GetDGSExpiredPurchases getInstance() {
        return GetDGSExpiredPurchasesHolder.INSTANCE;
    }

    private GetDGSExpiredPurchases() {
        super(new APITag[] {APITag.DGS}, "seller", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long sellerId = ParameterParser.getAccountId(req, "seller", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();

        try (DbIterator<DigitalGoodsStore.Purchase> purchases = DigitalGoodsStore.Purchase.getExpiredSellerPurchases(sellerId, firstIndex, lastIndex)) {
            while (purchases.hasNext()) {
                purchasesJSON.add(JSONData.purchase(purchases.next()));
            }
        }

        response.put("purchases", purchasesJSON);
        return response;
    }

}
