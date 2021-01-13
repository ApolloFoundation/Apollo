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
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetBuyOffers extends AbstractAPIRequestHandler {

    public GetBuyOffers() {
        super(new APITag[]{APITag.MS}, "currency", "account", "availableOnly", "firstIndex", "lastIndex");
    }

    private CurrencyExchangeOfferFacade exchangeOfferFacade;

    public synchronized CurrencyExchangeOfferFacade lookupCurrencyExchangeFacade() {
        if (exchangeOfferFacade == null) {
            exchangeOfferFacade = CDI.current().select(CurrencyExchangeOfferFacade.class).get();
        }
        return exchangeOfferFacade;
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long currencyId = HttpParameterParserUtil.getUnsignedLong(req, "currency", false);
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        if (currencyId == 0 && accountId == 0) {
            return JSONResponses.MISSING_CURRENCY_ACCOUNT;
        }
        boolean availableOnly = "true".equalsIgnoreCase(req.getParameter("availableOnly"));

        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray offerData = new JSONArray();
        response.put("offers", offerData);

        DbIterator<CurrencyBuyOffer> offers = null;
        try {
            if (accountId == 0) {
                offers = lookupCurrencyExchangeFacade().getCurrencyBuyOfferService()
                    .getCurrencyOffers(currencyId, availableOnly, firstIndex, lastIndex);
            } else if (currencyId == 0) {
                offers = lookupCurrencyExchangeFacade().getCurrencyBuyOfferService()
                    .getAccountOffers(accountId, availableOnly, firstIndex, lastIndex);
            } else {
                CurrencyBuyOffer offer = lookupCurrencyExchangeFacade().getCurrencyBuyOfferService()
                    .getOffer(currencyId, accountId);
                if (offer != null) {
                    offerData.add(JSONData.offer(offer));
                }
                return response;
            }
            while (offers.hasNext()) {
                offerData.add(JSONData.offer(offers.next()));
            }
        } finally {
            DbUtils.close(offers);
        }

        return response;
    }

}
