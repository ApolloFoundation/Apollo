/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyDeletion;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class MSCurrencyDeletion extends MonetarySystem {
    
    public MSCurrencyDeletion() {
    }

    @Override
    public byte getSubtype() {
        return SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_DELETION;
    }

    @Override
    public String getName() {
        return "CurrencyDeletion";
    }

    @Override
    public MonetarySystemCurrencyDeletion parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemCurrencyDeletion(buffer);
    }

    @Override
    public MonetarySystemCurrencyDeletion parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemCurrencyDeletion(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        MonetarySystemCurrencyDeletion attachment = (MonetarySystemCurrencyDeletion) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        String nameLower = currency.getName().toLowerCase();
        String codeLower = currency.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, true);
        if (!nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, true);
        }
        return isDuplicate;
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyDeletion attachment = (MonetarySystemCurrencyDeletion) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        CurrencyType.validate(currency, transaction);
        if (!currency.canBeDeletedBy(transaction.getSenderId())) {
            throw new AplException.NotCurrentlyValidException("Currency " + Long.toUnsignedString(currency.getId()) + " cannot be deleted by account " + Long.toUnsignedString(transaction.getSenderId()));
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
        MonetarySystemCurrencyDeletion attachment = (MonetarySystemCurrencyDeletion) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        currency.delete(getLedgerEvent(), transaction.getId(), senderAccount);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }
    
}
