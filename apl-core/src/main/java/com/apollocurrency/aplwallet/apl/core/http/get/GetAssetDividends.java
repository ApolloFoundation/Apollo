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

import com.apollocurrency.aplwallet.apl.core.monetary.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAssetDividends extends AbstractAPIRequestHandler {

    public GetAssetDividends() {
        super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex", "timestamp");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", false);
        int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray dividendsData = new JSONArray();
        try (DbIterator<AssetDividend> dividends = AssetDividend.getAssetDividends(assetId, firstIndex, lastIndex)) {
            while (dividends.hasNext()) {
                AssetDividend assetDividend = dividends.next();
                if (assetDividend.getTimestamp() < timestamp) {
                    break;
                }
                dividendsData.add(JSONData.assetDividend(assetDividend));
            }
        }
        response.put("dividends", dividendsData);
        return response;
    }

}
