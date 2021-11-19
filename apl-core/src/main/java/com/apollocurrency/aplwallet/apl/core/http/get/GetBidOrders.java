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

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.impl.BidOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCBidOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.CollectorUtils;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@Vetoed
public final class GetBidOrders extends AbstractAPIRequestHandler {
    private final OrderService<BidOrder, CCBidOrderPlacementAttachment> bidOrderService =
        CDI.current().select(BidOrderServiceImpl.class, BidOrderService.Literal.INSTANCE).get();

    public GetBidOrders() {
        super(new APITag[]{APITag.AE}, "asset", "firstIndex", "lastIndex", "showExpectedCancellations");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = HttpParameterParserUtil.getUnsignedLong(req, "asset", true);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        boolean showExpectedCancellations = "true".equalsIgnoreCase(req.getParameter("showExpectedCancellations"));

        long[] cancellations = null;
        if (showExpectedCancellations) {
            Filter<Transaction> filter = transaction -> transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.CC_BID_ORDER_CANCELLATION;
            List<Transaction> transactions = lookupBlockchainProcessor().getExpectedTransactions(filter);
            cancellations = new long[transactions.size()];
            for (int i = 0; i < transactions.size(); i++) {
                ColoredCoinsOrderCancellationAttachment attachment = (ColoredCoinsOrderCancellationAttachment) transactions.get(i).getAttachment();
                cancellations[i] = attachment.getOrderId();
            }
            Arrays.sort(cancellations);
        }

        long[] finalCancellations = cancellations;
        JSONArray orders = bidOrderService.getSortedOrders(assetId, firstIndex, lastIndex)
            .map(bidOrder -> getJsonObject(showExpectedCancellations, finalCancellations, bidOrder))
            .collect(CollectorUtils.jsonCollector());

        JSONObject response = new JSONObject();
        response.put("bidOrders", orders);
        return response;
    }

    private JSONObject getJsonObject(boolean showExpectedCancellations, long[] finalCancellations, BidOrder bidOrder) {
        JSONObject orderJSON = JSONData.bidOrder(bidOrder);
        if (showExpectedCancellations && Arrays.binarySearch(finalCancellations, bidOrder.getId()) >= 0) {
            orderJSON.put("expectedCancellation", Boolean.TRUE);
        }
        return orderJSON;
    }
}
