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

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeAttachment;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public final class GetExpectedExchangeRequests extends AbstractAPIRequestHandler {

    public GetExpectedExchangeRequests() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.MS}, "account", "currency", "includeCurrencyInfo");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long accountId = HttpParameterParserUtil.getAccountId(req, "account", false);
        long currencyId = HttpParameterParserUtil.getUnsignedLong(req, "currency", false);
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        Filter<Transaction> filter = transaction -> {
            if (transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.MS_EXCHANGE_BUY && transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.MS_EXCHANGE_SELL) {
                return false;
            }
            if (accountId != 0 && transaction.getSenderId() != accountId) {
                return false;
            }
            MonetarySystemExchangeAttachment attachment = (MonetarySystemExchangeAttachment) transaction.getAttachment();
            return currencyId == 0 || attachment.getCurrencyId() == currencyId;
        };

        List<? extends Transaction> transactions = lookupBlockchainProcessor().getExpectedTransactions(filter);

        JSONArray exchangeRequests = new JSONArray();
        transactions.forEach(transaction -> exchangeRequests.add(JSONData.expectedExchangeRequest(transaction, includeCurrencyInfo)));
        JSONObject response = new JSONObject();
        response.put("exchangeRequests", exchangeRequests);
        return response;

    }

}
