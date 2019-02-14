/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeSell;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
        if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getUnits()) {
            senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemExchangeSell attachment = (MonetarySystemExchangeSell) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemExchangeSell attachment = (MonetarySystemExchangeSell) transaction.getAttachment();
        ExchangeRequest.addExchangeRequest(transaction, attachment);
        CurrencyExchangeOffer.exchangeCurrencyForAPL(transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateATM(), attachment.getUnits());
    }
    
}
