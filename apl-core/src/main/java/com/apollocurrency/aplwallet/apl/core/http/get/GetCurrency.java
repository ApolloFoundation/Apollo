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

import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_CURRENCY;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_CURRENCY;

public final class GetCurrency extends AbstractAPIRequestHandler {

    private static class GetCurrencyHolder {
        private static final GetCurrency INSTANCE = new GetCurrency();
    }

    public static GetCurrency getInstance() {
        return GetCurrencyHolder.INSTANCE;
    }

    private GetCurrency() {
        super(new APITag[] {APITag.MS}, "currency", "code", "includeCounts");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        boolean includeCounts = "true".equalsIgnoreCase(req.getParameter("includeCounts"));
        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        Currency currency;
        if (currencyId == 0) {
            String currencyCode = Convert.emptyToNull(req.getParameter("code"));
            if (currencyCode == null) {
                return MISSING_CURRENCY;
            }
            currency = Currency.getCurrencyByCode(currencyCode);
        } else {
            currency = Currency.getCurrency(currencyId);
        }
        if (currency == null) {
            throw new ParameterException(UNKNOWN_CURRENCY);
        }
        return JSONData.currency(currency, includeCounts);
    }

}
