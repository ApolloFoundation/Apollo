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
import com.apollocurrency.aplwallet.apl.core.entity.operation.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.service.operation.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.operation.order.impl.AskOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.operation.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.utils.CollectorUtils;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

@Vetoed
public final class GetAccountCurrentAskOrders extends AbstractAPIRequestHandler {
    private final OrderService<AskOrder, ColoredCoinsAskOrderPlacement> askOrderService =
        CDI.current().select(AskOrderServiceImpl.class, AskOrderService.Literal.INSTANCE).get();

    public GetAccountCurrentAskOrders() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.AE}, "account", "asset", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long accountId = HttpParameterParserUtil.getAccountId(req, true);
        long assetId = HttpParameterParserUtil.getUnsignedLong(req, "asset", false);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        Stream<AskOrder> askOrders;
        if (assetId == 0) {
            askOrders = askOrderService.getOrdersByAccount(accountId, firstIndex, lastIndex);
        } else {
            askOrders = askOrderService.getOrdersByAccountAsset(accountId, assetId, firstIndex, lastIndex);
        }
        JSONArray orders = askOrders.map(JSONData::askOrder)
            .collect(CollectorUtils.jsonCollector());

        JSONObject response = new JSONObject();
        response.put("askOrders", orders);
        return response;
    }

}
