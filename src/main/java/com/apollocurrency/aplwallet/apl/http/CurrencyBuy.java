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

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Currency;

/**
 * Buy currency for APL
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>rateATM - exchange rate between APL amount and currency units
 * <li>units - number of units to buy
 * </ul>
 *
 * <p>
 * currency buy transaction attempts to match existing exchange offers. When a match is found, the minimum number of units
 * between the number of units offered and the units requested are exchanged at a rate matching the highest sell offer<br>
 * A single transaction can match multiple sell offers or none.
 * Unlike asset bid order, currency buy is not saved. It's either executed immediately (fully or partially) or not executed
 * at all.
 * For every match between buyer and seller an exchange record is saved, exchange records can be retrieved using the {@link GetExchanges} API
 */
public final class CurrencyBuy extends CreateTransaction {

    private static class CurrencyBuyHolder {
        private static final CurrencyBuy INSTANCE = new CurrencyBuy();
    }

    public static CurrencyBuy getInstance() {
        return CurrencyBuyHolder.INSTANCE;
    }

    private CurrencyBuy() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "rateATM", "units");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        Currency currency = ParameterParser.getCurrency(req);
        long rateATM = ParameterParser.getLong(req, "rateATM", 0, Long.MAX_VALUE, true);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req, validate);

        Attachment attachment = new Attachment.MonetarySystemExchangeBuy(currency.getId(), rateATM, units);
        return new CreateTransactionRequestData(attachment, account, JSONResponses.NOT_ENOUGH_FUNDS);
    }

}
