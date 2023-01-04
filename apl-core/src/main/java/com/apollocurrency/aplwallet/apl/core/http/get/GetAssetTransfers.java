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

import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAssetTransfers extends AbstractAPIRequestHandler {

    public GetAssetTransfers() {
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
        JSONArray transfersData = new JSONArray();
        DbIterator<AssetTransfer> transfers = null;
        try {
            if (accountId == 0) {
                transfers = lookupAssetTransferService().getAssetTransfers(assetId, firstIndex, lastIndex);
            } else if (assetId == 0) {
                transfers = lookupAssetTransferService().getAccountAssetTransfers(accountId, firstIndex, lastIndex);
            } else {
                transfers = lookupAssetTransferService().getAccountAssetTransfers(accountId, assetId, firstIndex, lastIndex);
            }
            while (transfers.hasNext()) {
                AssetTransfer assetTransfer = transfers.next();
                if (assetTransfer.getTimestamp() < timestamp) {
                    break;
                }
                transfersData.add(JSONData.assetTransfer(assetTransfer, includeAssetInfo));
            }
        } finally {
            DbUtils.close(transfers);
        }
        response.put("transfers", transfersData);

        return response;
    }

//    @Override
//    protected boolean startDbTransaction() {
//        return true;
//    }
}
