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

import com.apollocurrency.aplwallet.apl.core.dgs.DGSService;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetDGSPurchaseCount extends AbstractAPIRequestHandler {

    private DGSService service = CDI.current().select(DGSService.class).get();

    public GetDGSPurchaseCount() {
        super(new APITag[]{APITag.DGS}, "seller", "buyer", "withPublicFeedbacksOnly", "completed");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long sellerId = HttpParameterParserUtil.getAccountId(req, "seller", false);
        long buyerId = HttpParameterParserUtil.getAccountId(req, "buyer", false);
        final boolean completed = "true".equalsIgnoreCase(req.getParameter("completed"));
        final boolean withPublicFeedbacksOnly = "true".equalsIgnoreCase(req.getParameter("withPublicFeedbacksOnly"));

        JSONObject response = new JSONObject();
        int count;
        if (sellerId != 0 && buyerId == 0) {
            count = service.getSellerPurchaseCount(sellerId, withPublicFeedbacksOnly, completed);
        } else if (sellerId == 0 && buyerId != 0) {
            count = service.getBuyerPurchaseCount(buyerId, withPublicFeedbacksOnly, completed);
        } else if (sellerId == 0 && buyerId == 0) {
            count = service.getPurchaseCount(withPublicFeedbacksOnly, completed);
        } else {
            count = service.getSellerBuyerPurchaseCount(sellerId, buyerId, withPublicFeedbacksOnly, completed);
        }
        response.put("numberOfPurchases", count);
        return response;
    }

}
