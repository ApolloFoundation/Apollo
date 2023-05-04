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

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.AvailableOffers;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAvailableToSell extends AbstractAPIRequestHandler {

    public GetAvailableToSell() {
        super(new APITag[]{APITag.MS}, "currency", "units");
    }

    private CurrencyExchangeOfferFacade currencyExchangeOfferFacade = CDI.current().select(CurrencyExchangeOfferFacade.class).get();
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long currencyId = HttpParameterParserUtil.getUnsignedLong(req, "currency", true);
        long units = HttpParameterParserUtil.getLong(req, "units", 1L, Long.MAX_VALUE, true);
        AvailableOffers availableOffers = currencyExchangeOfferFacade.getAvailableToSell(currencyId, units);
        return JSONData.availableOffers(availableOffers);
    }

}
