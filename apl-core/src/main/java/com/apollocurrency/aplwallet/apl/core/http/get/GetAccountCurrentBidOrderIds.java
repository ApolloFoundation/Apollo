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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.impl.BidOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCBidOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.CollectorUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

@Vetoed
public final class GetAccountCurrentBidOrderIds extends AbstractAPIRequestHandler {
    private final OrderService<BidOrder, CCBidOrderPlacementAttachment> bidOrderService =
        CDI.current().select(BidOrderServiceImpl.class, BidOrderService.Literal.INSTANCE).get();

    public GetAccountCurrentBidOrderIds() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.AE}, "account", "asset", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long accountId = HttpParameterParserUtil.getAccountId(req, true);
        long assetId = HttpParameterParserUtil.getUnsignedLong(req, "asset", false);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        Stream<BidOrder> bidOrders;
        if (assetId == 0) {
            bidOrders = bidOrderService.getOrdersByAccount(accountId, firstIndex, lastIndex);
        } else {
            bidOrders = bidOrderService.getOrdersByAccountAsset(accountId, assetId, firstIndex, lastIndex);
        }
        JSONArray orderIds = bidOrders.map(b -> Long.toUnsignedString(b.getId()))
            .collect(CollectorUtils.jsonCollector());

        JSONObject response = new JSONObject();
        response.put("bidOrderIds", orderIds);
        return response;
    }

}
