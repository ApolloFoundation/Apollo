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
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetCurrencyTransfers extends AbstractAPIRequestHandler {

    public GetCurrencyTransfers() {
        super(new APITag[]{APITag.MS}, "currency", "account", "firstIndex", "lastIndex", "timestamp", "includeCurrencyInfo");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long currencyId = HttpParameterParserUtil.getUnsignedLong(req, "currency", false);
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        if (currencyId == 0 && accountId == 0) {
            return JSONResponses.MISSING_CURRENCY_ACCOUNT;
        }
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));
        int timestamp = HttpParameterParserUtil.getTimestamp(req);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray transfersData = new JSONArray();
        DbIterator<CurrencyTransfer> transfers = null;
        try {
            if (accountId == 0) {
                transfers = lookupCurrencyTransferService().getCurrencyTransfers(currencyId, firstIndex, lastIndex);
            } else if (currencyId == 0) {
                transfers = lookupCurrencyTransferService().getAccountCurrencyTransfers(accountId, firstIndex, lastIndex);
            } else {
                transfers = lookupCurrencyTransferService().getAccountCurrencyTransfers(accountId, currencyId, firstIndex, lastIndex);
            }
            while (transfers.hasNext()) {
                CurrencyTransfer currencyTransfer = transfers.next();
                if (currencyTransfer.getTimestamp() < timestamp) {
                    break;
                }
                transfersData.add(JSONData.currencyTransfer(currencyTransfer, includeCurrencyInfo));
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
