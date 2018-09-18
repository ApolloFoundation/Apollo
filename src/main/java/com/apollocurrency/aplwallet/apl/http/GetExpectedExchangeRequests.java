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

import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.MonetarySystem;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetExpectedExchangeRequests extends APIServlet.APIRequestHandler {

    private static class GetExpectedExchangeRequestsHolder {
        private static final GetExpectedExchangeRequests INSTANCE = new GetExpectedExchangeRequests();
    }

    public static GetExpectedExchangeRequests getInstance() {
        return GetExpectedExchangeRequestsHolder.INSTANCE;
    }

    private GetExpectedExchangeRequests() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.MS}, "account", "currency", "includeCurrencyInfo");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long accountId = ParameterParser.getAccountId(req, "account", false);
        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        Filter<Transaction> filter = transaction -> {
            if (transaction.getType() != MonetarySystem.EXCHANGE_BUY && transaction.getType() != MonetarySystem.EXCHANGE_SELL) {
                return false;
            }
            if (accountId != 0 && transaction.getSenderId() != accountId) {
                return false;
            }
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange) transaction.getAttachment();
            return currencyId == 0 || attachment.getCurrencyId() == currencyId;
        };

        List<? extends Transaction> transactions = Apl.getBlockchain().getExpectedTransactions(filter);

        JSONArray exchangeRequests = new JSONArray();
        transactions.forEach(transaction -> exchangeRequests.add(JSONData.expectedExchangeRequest(transaction, includeCurrencyInfo)));
        JSONObject response = new JSONObject();
        response.put("exchangeRequests", exchangeRequests);
        return response;

    }

}
