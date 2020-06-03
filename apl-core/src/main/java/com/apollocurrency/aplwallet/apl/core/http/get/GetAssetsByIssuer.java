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
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.monetary.model.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.service.AssetService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAssetsByIssuer extends AbstractAPIRequestHandler {

    public GetAssetsByIssuer() {
        super(new APITag[]{APITag.AE, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex", "includeCounts");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long[] accountIds = HttpParameterParserUtil.getAccountIds(req, true);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        boolean includeCounts = "true".equalsIgnoreCase(req.getParameter("includeCounts"));

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        response.put("assets", accountsJSONArray);
        AssetService assetService = CDI.current().select(AssetService.class).get();
        for (long accountId : accountIds) {
            JSONArray assetsJSONArray = new JSONArray();
            try (DbIterator<Asset> assets = assetService.getAssetsIssuedBy(accountId, firstIndex, lastIndex)) {
                while (assets.hasNext()) {
                    assetsJSONArray.add(JSONData.asset(assets.next(), includeCounts));
                }
            }
            accountsJSONArray.add(assetsJSONArray);
        }
        return response;
    }

}
