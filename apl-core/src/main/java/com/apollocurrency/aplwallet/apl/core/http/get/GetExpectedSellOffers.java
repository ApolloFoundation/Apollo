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
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Vetoed
public final class GetExpectedSellOffers extends AbstractAPIRequestHandler {

    private final Comparator<Transaction> rateComparator = (o1, o2) -> {
        MSPublishExchangeOfferAttachment a1 = (MSPublishExchangeOfferAttachment) o1.getAttachment();
        MSPublishExchangeOfferAttachment a2 = (MSPublishExchangeOfferAttachment) o2.getAttachment();
        return Long.compare(a1.getSellRateATM(), a2.getSellRateATM());
    };

    public GetExpectedSellOffers() {
        super(new APITag[]{APITag.MS}, "currency", "account", "sortByRate");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long currencyId = HttpParameterParserUtil.getUnsignedLong(req, "currency", false);
        long accountId = HttpParameterParserUtil.getAccountId(req, "account", false);
        boolean sortByRate = "true".equalsIgnoreCase(req.getParameter("sortByRate"));

        Filter<Transaction> filter = transaction -> {
            if (transaction.isNotOfType(TransactionTypes.TransactionTypeSpec.MS_PUBLISH_EXCHANGE_OFFER)) {
                return false;
            }
            if (accountId != 0 && transaction.getSenderId() != accountId) {
                return false;
            }
            MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
            return currencyId == 0 || attachment.getCurrencyId() == currencyId;
        };

        List<? extends Transaction> transactions = lookupBlockchainProcessor().getExpectedTransactions(filter);
        if (sortByRate) {
            Collections.sort(transactions, rateComparator);
        }

        JSONObject response = new JSONObject();
        JSONArray offerData = new JSONArray();
        transactions.forEach(transaction -> offerData.add(JSONData.expectedSellOffer(transaction)));
        response.put("offers", offerData);
        return response;
    }

}
