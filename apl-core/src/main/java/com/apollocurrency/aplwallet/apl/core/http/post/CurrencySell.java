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

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.get.GetExchanges;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeSell;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

/**
 * Sell currency for APL
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>rateATM - exchange rate between APL amount and currency units
 * <li>units - number of units to sell
 * </ul>
 *
 * <p>
 * currency sell transaction attempts to match existing exchange offers. When a match is found, the minimum number of units
 * between the number of units offered and the units requested are exchanged at a rate matching the lowest buy offer<br>
 * A single transaction can match multiple buy offers or none.
 * Unlike asset ask order, currency sell is not saved. It's either executed immediately (fully or partially) or not executed
 * at all.
 * For every match between buyer and seller an exchange record is saved, exchange records can be retrieved using the {@link GetExchanges} API
 */
@Vetoed
public final class CurrencySell extends CreateTransaction {

    public CurrencySell() {
        super(new APITag[]{APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "rateATM", "units");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Currency currency = HttpParameterParserUtil.getCurrency(req);
        long rateATM = HttpParameterParserUtil.getLong(req, "rateATM", 0, Long.MAX_VALUE, true);
        long units = HttpParameterParserUtil.getLong(req, "units", 0, Long.MAX_VALUE, true);
        Account account = HttpParameterParserUtil.getSenderAccount(req);

        Attachment attachment = new MonetarySystemExchangeSell(currency.getId(), rateATM, units);
        try {
            return createTransaction(req, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return JSONResponses.NOT_ENOUGH_CURRENCY;
        }
    }

}
