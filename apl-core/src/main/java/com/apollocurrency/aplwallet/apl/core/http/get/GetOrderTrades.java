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
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.trade.entity.Trade;
import com.apollocurrency.aplwallet.apl.core.trade.service.TradeService;
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
public final class GetOrderTrades extends AbstractAPIRequestHandler {
    private final TradeService tradeService = CDI.current().select(TradeService.class).get();

    public GetOrderTrades() {
        super(new APITag[]{APITag.AE}, "askOrder", "bidOrder", "includeAssetInfo", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long askOrderId = HttpParameterParserUtil.getUnsignedLong(req, "askOrder", false);
        long bidOrderId = HttpParameterParserUtil.getUnsignedLong(req, "bidOrder", false);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        if (askOrderId == 0 && bidOrderId == 0) {
            return JSONResponses.missing("askOrder", "bidOrder");
        }

        JSONObject response = new JSONObject();
        JSONArray tradesData;
        if (askOrderId != 0 && bidOrderId != 0) {
            tradesData = new JSONArray();
            Trade trade = tradeService.getTrade(askOrderId, bidOrderId);
            if (trade != null) {
                tradesData.add(JSONData.trade(trade, includeAssetInfo));
            }
        } else {
            Stream<Trade> trades;
            if (askOrderId != 0) {
                trades = tradeService.getAskOrderTrades(askOrderId, firstIndex, lastIndex);
            } else {
                trades = tradeService.getBidOrderTrades(bidOrderId, firstIndex, lastIndex);
            }
            tradesData = trades.map(t -> JSONData.trade(t, includeAssetInfo))
                .collect(CollectorUtils.jsonCollector());
        }
        response.put("trades", tradesData);

        return response;
    }
}
