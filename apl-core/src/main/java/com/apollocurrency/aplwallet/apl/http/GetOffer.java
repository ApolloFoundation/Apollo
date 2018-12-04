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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.CurrencySellOffer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetOffer extends APIServlet.APIRequestHandler {

    private static class GetOfferHolder {
        private static final GetOffer INSTANCE = new GetOffer();
    }

    public static GetOffer getInstance() {
        return GetOfferHolder.INSTANCE;
    }

    private GetOffer() {
        super(new APITag[] {APITag.MS}, "offer");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        JSONObject response = new JSONObject();
        CurrencyBuyOffer buyOffer = ParameterParser.getBuyOffer(req);
        CurrencySellOffer sellOffer = ParameterParser.getSellOffer(req);
        response.put("buyOffer", JSONData.offer(buyOffer));
        response.put("sellOffer", JSONData.offer(sellOffer));
        return response;
    }

}
