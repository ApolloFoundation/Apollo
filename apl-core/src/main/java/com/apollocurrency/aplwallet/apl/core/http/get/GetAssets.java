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

import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_ASSET;

public final class GetAssets extends AbstractAPIRequestHandler {

    public GetAssets() {
        super(new APITag[]{APITag.AE}, "assets", "assets", "assets", "includeCounts"); // limit to 3 for testing
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long[] assetIds = HttpParameterParserUtil.getUnsignedLongs(req, "assets");
        boolean includeCounts = "true".equalsIgnoreCase(req.getParameter("includeCounts"));
        JSONObject response = new JSONObject();
        JSONArray assetsJSONArray = new JSONArray();
        response.put("assets", assetsJSONArray);
        AssetService assetService = CDI.current().select(AssetService.class).get();
        for (long assetId : assetIds) {
            Asset asset = assetService.getAsset(assetId);
            if (asset == null) {
                return UNKNOWN_ASSET;
            }
            assetsJSONArray.add(JSONData.asset(asset, includeCounts));
        }
        return response;
    }

}
