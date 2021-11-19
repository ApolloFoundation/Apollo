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
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSCurrencyTransferAttachment;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public final class GetExpectedCurrencyTransfers extends AbstractAPIRequestHandler {

    public GetExpectedCurrencyTransfers() {
        super(new APITag[]{APITag.MS}, "currency", "account", "includeCurrencyInfo");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long currencyId = HttpParameterParserUtil.getUnsignedLong(req, "currency", false);
        long accountId = HttpParameterParserUtil.getAccountId(req, "account", false);
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        Filter<Transaction> filter = transaction -> {
            if (transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.MS_CURRENCY_TRANSFER) {
                return false;
            }
            if (accountId != 0 && transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId) {
                return false;
            }
            MSCurrencyTransferAttachment attachment = (MSCurrencyTransferAttachment) transaction.getAttachment();
            return currencyId == 0 || attachment.getCurrencyId() == currencyId;
        };

        List<Transaction> transactions = lookupBlockchainProcessor().getExpectedTransactions(filter);

        JSONObject response = new JSONObject();
        JSONArray transfersData = new JSONArray();
        transactions.forEach(transaction -> transfersData.add(JSONData.expectedCurrencyTransfer(transaction, includeCurrencyInfo)));
        response.put("transfers", transfersData);

        return response;
    }

}
