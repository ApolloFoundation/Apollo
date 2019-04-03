/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.mint.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.app.mint.CurrencyMinting;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class MSCurrencyMinting extends MonetarySystem {
    
    public MSCurrencyMinting() {
    }

    @Override
    public byte getSubtype() {
        return SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_MINTING;
    }

    @Override
    public String getName() {
        return "CurrencyMinting";
    }

    @Override
    public MonetarySystemCurrencyMinting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemCurrencyMinting(buffer);
    }

    @Override
    public MonetarySystemCurrencyMinting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemCurrencyMinting(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyMinting attachment = (MonetarySystemCurrencyMinting) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        CurrencyType.validate(currency, transaction);
        if (attachment.getUnits() <= 0) {
            throw new AplException.NotValidException("Invalid number of units: " + attachment.getUnits());
        }
        if (attachment.getUnits() > (currency.getMaxSupply() - currency.getReserveSupply()) / Constants.MAX_MINTING_RATIO) {
            throw new AplException.NotValidException(String.format("Cannot mint more than 1/%d of the total units supply in a single request", Constants.MAX_MINTING_RATIO));
        }
        if (!currency.isActive()) {
            throw new AplException.NotCurrentlyValidException("Currency not currently active " + attachment.getJSONObject());
        }
        long counter = CurrencyMint.getCounter(attachment.getCurrencyId(), transaction.getSenderId());
        if (attachment.getCounter() <= counter) {
            throw new AplException.NotCurrentlyValidException(String.format("Counter %d has to be bigger than %d", attachment.getCounter(), counter));
        }
        if (!CurrencyMinting.meetsTarget(transaction.getSenderId(), currency, attachment)) {
            throw new AplException.NotCurrentlyValidException(String.format("Hash doesn't meet target %s", attachment.getJSONObject()));
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemCurrencyMinting attachment = (MonetarySystemCurrencyMinting) transaction.getAttachment();
        CurrencyMint.mintCurrency(getLedgerEvent(), transaction.getId(), senderAccount, attachment);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        MonetarySystemCurrencyMinting attachment = (MonetarySystemCurrencyMinting) transaction.getAttachment();
        return TransactionType.isDuplicate(CURRENCY_MINTING, attachment.getCurrencyId() + ":" + transaction.getSenderId(), duplicates, true) || super.isDuplicate(transaction, duplicates);
    }

    @Override
    public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        MonetarySystemCurrencyMinting attachment = (MonetarySystemCurrencyMinting) transaction.getAttachment();
        return TransactionType.isDuplicate(CURRENCY_MINTING, attachment.getCurrencyId() + ":" + transaction.getSenderId(), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }
    
}
