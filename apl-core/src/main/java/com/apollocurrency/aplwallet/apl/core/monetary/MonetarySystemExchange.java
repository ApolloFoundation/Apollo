/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeAttachment;

/**
 * @author al
 */
public abstract class MonetarySystemExchange extends MonetarySystem {

    @Override
    public final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemExchangeAttachment attachment = (MonetarySystemExchangeAttachment) transaction.getAttachment();
        if (attachment.getRateATM() <= 0 || attachment.getUnits() == 0) {
            throw new AplException.NotValidException("Invalid exchange: " + attachment.getJSONObject());
        }
        CurrencyService currencyService = lookupCurrencyService();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
        if (!currencyService.isActive(currency)) {
            throw new AplException.NotCurrentlyValidException("Currency not active: " + attachment.getJSONObject());
        }
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

}
