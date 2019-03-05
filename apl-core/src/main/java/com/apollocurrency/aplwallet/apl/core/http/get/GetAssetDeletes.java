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

import com.apollocurrency.aplwallet.apl.core.monetary.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetDeletes extends AbstractAPIRequestHandler {

    private static class GetAssetDeletesHolder {
        private static final GetAssetDeletes INSTANCE = new GetAssetDeletes();
    }

    public static GetAssetDeletes getInstance() {
        return GetAssetDeletesHolder.INSTANCE;
    }

    private GetAssetDeletes() {
        super(new APITag[] {APITag.AE}, "asset", "account", "firstIndex", "lastIndex", "timestamp", "includeAssetInfo");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", false);
        long accountId = ParameterParser.getAccountId(req, false);
        if (assetId == 0 && accountId == 0) {
            return JSONResponses.MISSING_ASSET_ACCOUNT;
        }
        int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray deletesData = new JSONArray();
        DbIterator<AssetDelete> deletes = null;
        try {
            if (accountId == 0) {
                deletes = AssetDelete.getAssetDeletes(assetId, firstIndex, lastIndex);
            } else if (assetId == 0) {
                deletes = AssetDelete.getAccountAssetDeletes(accountId, firstIndex, lastIndex);
            } else {
                deletes = AssetDelete.getAccountAssetDeletes(accountId, assetId, firstIndex, lastIndex);
            }
            while (deletes.hasNext()) {
                AssetDelete assetDelete = deletes.next();
                if (assetDelete.getTimestamp() < timestamp) {
                    break;
                }
                deletesData.add(JSONData.assetDelete(assetDelete, includeAssetInfo));
            }
        } finally {
            DbUtils.close(deletes);
        }
        response.put("deletes", deletesData);

        return response;
    }

    @Override
    protected boolean startDbTransaction() {
        return true;
    }
}
