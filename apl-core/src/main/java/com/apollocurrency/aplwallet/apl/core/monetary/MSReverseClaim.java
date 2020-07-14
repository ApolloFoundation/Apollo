/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveClaim;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
class MSReverseClaim extends MonetarySystem {

    public MSReverseClaim() {
    }

    @Override
    public byte getSubtype() {
        return SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_RESERVE_CLAIM;
    }

    @Override
    public String getName() {
        return "ReserveClaim";
    }

    @Override
    public MonetarySystemReserveClaim parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemReserveClaim(buffer);
    }

    @Override
    public MonetarySystemReserveClaim parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemReserveClaim(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        if (attachment.getUnits() <= 0) {
            throw new AplException.NotValidException("Reserve claim number of units must be positive: " + attachment.getUnits());
        }
        Currency currency = lookupCurrencyService().getCurrency(attachment.getCurrencyId());
        CurrencyType.validate(currency, transaction);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        if (lookupAccountCurrencyService().getUnconfirmedCurrencyUnits(senderAccount, attachment.getCurrencyId()) >= attachment.getUnits()) {
            lookupAccountCurrencyService().addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        Currency currency = lookupCurrencyService().getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            lookupAccountCurrencyService().addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        lookupCurrencyService().claimReserve(getLedgerEvent(), transaction.getId(), senderAccount, attachment.getCurrencyId(), attachment.getUnits());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
