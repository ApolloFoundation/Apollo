/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class MSCurrencyTransfer extends MonetarySystem {
    
    public MSCurrencyTransfer() {
    }

    @Override
    public byte getSubtype() {
        return SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_TRANSFER;
    }

    @Override
    public String getName() {
        return "CurrencyTransfer";
    }

    @Override
    public MonetarySystemCurrencyTransfer parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemCurrencyTransfer(buffer);
    }

    @Override
    public MonetarySystemCurrencyTransfer parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemCurrencyTransfer(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        if (attachment.getUnits() <= 0) {
            throw new AplException.NotValidException("Invalid currency transfer: " + attachment.getJSONObject());
        }
        if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
            throw new AplException.NotValidException("Currency transfer to genesis account not allowed");
        }
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        CurrencyType.validate(currency, transaction);
        if (!currency.isActive()) {
            throw new AplException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        if (attachment.getUnits() > senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId())) {
            return false;
        }
        senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        Currency.transferCurrency(getLedgerEvent(), transaction.getId(), senderAccount, recipientAccount, attachment.getCurrencyId(), attachment.getUnits());
        CurrencyTransfer.addTransfer(transaction, attachment);
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }
    
}
