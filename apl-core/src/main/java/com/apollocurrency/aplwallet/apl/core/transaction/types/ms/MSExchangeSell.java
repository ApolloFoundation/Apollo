/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeSell;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
class MSExchangeSell extends MonetarySystemExchange {

    public MSExchangeSell() {
    }

    @Override
    public byte getSubtype() {
        return SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_EXCHANGE_SELL;
    }

    @Override
    public String getName() {
        return "ExchangeSell";
    }

    @Override
    public MonetarySystemExchangeSell parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemExchangeSell(buffer);
    }

    @Override
    public MonetarySystemExchangeSell parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemExchangeSell(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemExchangeSell attachment = (MonetarySystemExchangeSell) transaction.getAttachment();
        if (lookupAccountCurrencyService().getUnconfirmedCurrencyUnits(senderAccount, attachment.getCurrencyId()) >= attachment.getUnits()) {
            lookupAccountCurrencyService().addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemExchangeSell attachment = (MonetarySystemExchangeSell) transaction.getAttachment();
        Currency currency = lookupCurrencyService().getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            lookupAccountCurrencyService().addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemExchangeSell attachment = (MonetarySystemExchangeSell) transaction.getAttachment();
        lookupExchangeRequestService().addExchangeRequest(transaction, attachment);
        lookupCurrencyExchangeOfferFacade().exchangeCurrencyForAPL(
            transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateATM(), attachment.getUnits());
    }

}
