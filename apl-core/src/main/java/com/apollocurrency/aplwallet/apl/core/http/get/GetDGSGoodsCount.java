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
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetDGSGoodsCount extends AbstractAPIRequestHandler {

    public GetDGSGoodsCount() {
        super(new APITag[] {APITag.DGS}, "seller", "inStockOnly");
    }
    private DGSService service = CDI.current().select(DGSService.class).get();

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long sellerId = HttpParameterParserUtil.getAccountId(req, "seller", false);
        boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));

        JSONObject response = new JSONObject();
        response.put("numberOfGoods", sellerId != 0
                ? service.getSellerGoodsCount(sellerId, inStockOnly)
                : inStockOnly ? service.getGoodsCountInStock() : service.getGoodsCount());
        return response;
    }

}
