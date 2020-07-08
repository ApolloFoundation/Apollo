/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeAttachment;
import com.apollocurrency.aplwallet.apl.core.app.AplException;

/**
 * @author al
 */
public abstract class MonetarySystemExchange extends MonetarySystemTransactionType {

    @Override
    public final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemExchangeAttachment attachment = (MonetarySystemExchangeAttachment) transaction.getAttachment();
        if (attachment.getRateATM() <= 0 || attachment.getUnits() == 0) {
            throw new AplException.NotValidException("Invalid exchange: " + attachment.getJSONObject());
        }
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        CurrencyType.validate(currency, transaction);
        if (!currency.isActive()) {
            throw new AplException.NotCurrentlyValidException("Currency not active: " + attachment.getJSONObject());
        }
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

}
