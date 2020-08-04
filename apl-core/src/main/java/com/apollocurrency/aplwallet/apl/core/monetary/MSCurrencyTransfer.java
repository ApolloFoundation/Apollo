/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
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
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Currency transfer to genesis account not allowed");
        }
        CurrencyService currencyService = lookupCurrencyService();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
        if (!currencyService.isActive(currency)) {
            throw new AplException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        if (attachment.getUnits() > lookupAccountCurrencyService().getUnconfirmedCurrencyUnits(senderAccount, attachment.getCurrencyId())) {
            return false;
        }
        lookupAccountCurrencyService().addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        Currency currency = lookupCurrencyService().getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            lookupAccountCurrencyService().addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        lookupCurrencyService().transferCurrency(getLedgerEvent(), transaction.getId(), senderAccount, recipientAccount, attachment.getCurrencyId(), attachment.getUnits());
        lookupCurrencyTransferService().addTransfer(transaction, attachment);
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

}
