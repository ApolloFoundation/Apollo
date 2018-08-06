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

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetAccounts extends APIServlet.APIRequestHandler {

    private static class GetAssetAccountsHolder {
        private static final GetAssetAccounts INSTANCE = new GetAssetAccounts();
    }

    public static GetAssetAccounts getInstance() {
        return GetAssetAccountsHolder.INSTANCE;
    }

    private GetAssetAccounts() {
        super(new APITag[] {APITag.AE}, "asset", "height", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = ParameterParser.getUnsignedLong(req, "asset", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        int height = ParameterParser.getHeight(req);

        JSONArray accountAssets = new JSONArray();
        try (DbIterator<Account.AccountAsset> iterator = Account.getAssetAccounts(assetId, height, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Account.AccountAsset accountAsset = iterator.next();
                accountAssets.add(JSONData.accountAsset(accountAsset, true, false));
            }
        }

        JSONObject response = new JSONObject();
        response.put("accountAssets", accountAssets);
        return response;

    }

}
