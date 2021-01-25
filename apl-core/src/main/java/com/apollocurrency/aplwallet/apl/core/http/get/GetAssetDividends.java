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

import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDividendService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public final class GetAssetDividends extends AbstractAPIRequestHandler {

    public GetAssetDividends() {
        super(new APITag[]{APITag.AE}, "asset", "firstIndex", "lastIndex", "timestamp");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = HttpParameterParserUtil.getUnsignedLong(req, "asset", false);
        int timestamp = HttpParameterParserUtil.getTimestamp(req);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray dividendsData = new JSONArray();
        AssetDividendService assetDividendService = CDI.current().select(AssetDividendService.class).get();
/*
        try (DbIterator<AssetDividend> dividends = AssetDividend.getAssetDividends(assetId, firstIndex, lastIndex)) {
            while (dividends.hasNext()) {
                AssetDividend assetDividend = dividends.next();
                if (assetDividend.getTimestamp() < timestamp) {
                    break;
                }
                dividendsData.add(JSONData.assetDividend(assetDividend));
            }
        }
*/
        List<AssetDividend> dividends = assetDividendService.getAssetDividends(assetId, firstIndex, lastIndex);
        for (int i = 0; i < dividends.size(); i++) {
            AssetDividend assetDividend = dividends.get(i);
            if (assetDividend.getTimestamp() < timestamp) {
                break;
            }
            dividendsData.add(JSONData.assetDividend(assetDividend));
        }
        response.put("dividends", dividendsData);
        return response;
    }

}
