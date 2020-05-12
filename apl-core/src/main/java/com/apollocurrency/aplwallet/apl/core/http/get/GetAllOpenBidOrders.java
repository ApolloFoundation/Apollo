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

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.order.entity.BidOrder;
import com.apollocurrency.aplwallet.apl.core.order.service.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.order.service.impl.BidOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.order.service.OrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.utils.CollectorUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAllOpenBidOrders extends AbstractAPIRequestHandler {
    private final OrderService<BidOrder, ColoredCoinsBidOrderPlacement> bidOrderService =
        CDI.current().select(BidOrderServiceImpl.class, BidOrderService.Literal.INSTANCE).get();

    public GetAllOpenBidOrders() {
        super(new APITag[]{APITag.AE}, "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONArray ordersData = bidOrderService.getAll(firstIndex, lastIndex)
            .map(JSONData::bidOrder)
            .collect(CollectorUtils.jsonCollector());

        response.put("openOrders", ordersData);
        return response;
    }
}
