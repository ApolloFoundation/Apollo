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

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAssetDeleteAttachment;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public final class GetExpectedAssetDeletes extends AbstractAPIRequestHandler {

    public GetExpectedAssetDeletes() {
        super(new APITag[]{APITag.AE}, "asset", "account", "includeAssetInfo");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long assetId = HttpParameterParserUtil.getUnsignedLong(req, "asset", false);
        long accountId = HttpParameterParserUtil.getAccountId(req, "account", false);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        Filter<Transaction> filter = transaction -> {
            if (transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.CC_ASSET_DELETE) {
                return false;
            }
            if (accountId != 0 && transaction.getSenderId() != accountId) {
                return false;
            }
            CCAssetDeleteAttachment attachment = (CCAssetDeleteAttachment) transaction.getAttachment();
            return assetId == 0 || attachment.getAssetId() == assetId;
        };

        List<Transaction> transactions = lookupBlockchainProcessor().getExpectedTransactions(filter);

        JSONObject response = new JSONObject();
        JSONArray deletesData = new JSONArray();
        transactions.forEach(transaction -> deletesData.add(JSONData.expectedAssetDelete(transaction, includeAssetInfo)));
        response.put("deletes", deletesData);

        return response;
    }

}
