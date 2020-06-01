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
import com.apollocurrency.aplwallet.apl.core.entity.operation.Trade;
import com.apollocurrency.aplwallet.apl.core.service.operation.TradeService;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

@Vetoed
public final class GetTrades extends AbstractAPIRequestHandler {
    private final TradeService tradeService = CDI.current().select(TradeService.class).get();

    public GetTrades() {
        super(new APITag[]{APITag.AE}, "asset", "account", "firstIndex", "lastIndex", "timestamp", "includeAssetInfo");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = HttpParameterParserUtil.getUnsignedLong(req, "asset", false);
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        if (assetId == 0 && accountId == 0) {
            return JSONResponses.MISSING_ASSET_ACCOUNT;
        }

        int timestamp = HttpParameterParserUtil.getTimestamp(req);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray tradesData = new JSONArray();
        Stream<Trade> trades = null;
        if (accountId == 0) {
            trades = tradeService.getAssetTrades(assetId, firstIndex, lastIndex);
        } else if (assetId == 0) {
            trades = tradeService.getAccountTrades(accountId, firstIndex, lastIndex);
        } else {
            trades = tradeService.getAccountAssetTrades(accountId, assetId, firstIndex, lastIndex);
        }
        trades
            .takeWhile(t -> t.getTimestamp() >= timestamp)
            .map(t -> JSONData.trade(t, includeAssetInfo))
            .collect(toCollection(JSONArray::new));

        response.put("trades", tradesData);

        return response;
    }
}
