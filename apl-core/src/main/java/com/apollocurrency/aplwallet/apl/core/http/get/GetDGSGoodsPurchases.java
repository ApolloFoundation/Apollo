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

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetDGSGoodsPurchases extends AbstractAPIRequestHandler {

    private DGSService service = CDI.current().select(DGSService.class).get();

    public GetDGSGoodsPurchases() {
        super(new APITag[]{APITag.DGS}, "goods", "buyer", "firstIndex", "lastIndex", "withPublicFeedbacksOnly", "completed");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long goodsId = HttpParameterParserUtil.getUnsignedLong(req, "goods", true);
        long buyerId = HttpParameterParserUtil.getAccountId(req, "buyer", false);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        final boolean withPublicFeedbacksOnly = "true".equalsIgnoreCase(req.getParameter("withPublicFeedbacksOnly"));
        final boolean completed = "true".equalsIgnoreCase(req.getParameter("completed"));


        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        response.put("purchases", purchasesJSON);

        try (DbIterator<DGSPurchase> iterator = service.getGoodsPurchases(goodsId,
            buyerId, withPublicFeedbacksOnly, completed, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                purchasesJSON.add(JSONData.purchase(service, iterator.next()));
            }
        }
        return response;
    }

}
