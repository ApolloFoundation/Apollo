/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class MSExchangeBuy extends MonetarySystemExchange {
    
    public MSExchangeBuy() {
    }

    @Override
    public byte getSubtype() {
        return SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_EXCHANGE_BUY;
    }

    @Override
    public String getName() {
        return "ExchangeBuy";
    }

    @Override
    public MonetarySystemExchangeBuyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemExchangeBuyAttachment(buffer);
    }

    @Override
    public MonetarySystemExchangeBuyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemExchangeBuyAttachment(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return super.isDuplicate(transaction, duplicates);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemExchangeBuyAttachment attachment = (MonetarySystemExchangeBuyAttachment) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getUnits(), attachment.getRateATM())) {
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -Math.multiplyExact(attachment.getUnits(), attachment.getRateATM()));
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemExchangeBuyAttachment attachment = (MonetarySystemExchangeBuyAttachment) transaction.getAttachment();
        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getUnits(), attachment.getRateATM()));
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemExchangeBuyAttachment attachment = (MonetarySystemExchangeBuyAttachment) transaction.getAttachment();
        ExchangeRequest.addExchangeRequest(transaction, attachment);
        CurrencyExchangeOffer.exchangeAPLForCurrency(transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateATM(), attachment.getUnits());
    }
    
}
