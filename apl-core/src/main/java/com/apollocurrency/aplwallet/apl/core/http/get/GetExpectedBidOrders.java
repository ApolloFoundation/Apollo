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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.transaction.ColoredCoins;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.enterprise.inject.Vetoed;

@Vetoed
public final class GetExpectedBidOrders extends AbstractAPIRequestHandler {

    public GetExpectedBidOrders() {
        super(new APITag[] {APITag.AE}, "asset", "sortByPrice");
    }

    private final Comparator<Transaction> priceComparator = (o1, o2) -> {
        ColoredCoinsOrderPlacementAttachment a1 = (ColoredCoinsOrderPlacementAttachment)o1.getAttachment();
        ColoredCoinsOrderPlacementAttachment a2 = (ColoredCoinsOrderPlacementAttachment)o2.getAttachment();
        return Long.compare(a2.getPriceATM(), a1.getPriceATM());
    };
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", false);
        boolean sortByPrice = "true".equalsIgnoreCase(req.getParameter("sortByPrice"));
        Filter<Transaction> filter = transaction -> {
            if (transaction.getType() != ColoredCoins.BID_ORDER_PLACEMENT) {
                return false;
            }
            ColoredCoinsOrderPlacementAttachment attachment = (ColoredCoinsOrderPlacementAttachment)transaction.getAttachment();
            return assetId == 0 || attachment.getAssetId() == assetId;
        };

        List<Transaction> transactions = lookupBlockchainProcessor().getExpectedTransactions(filter);
        if (sortByPrice) {
            Collections.sort(transactions, priceComparator);
        }
        JSONArray orders = new JSONArray();
        transactions.forEach(transaction -> orders.add(JSONData.expectedBidOrder(transaction)));
        JSONObject response = new JSONObject();
        response.put("bidOrders", orders);
        return response;

    }

}
