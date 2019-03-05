/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class MSPublishExchangeOffer extends MonetarySystem {
    
    public MSPublishExchangeOffer() {
    }

    @Override
    public byte getSubtype() {
        return SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_PUBLISH_EXCHANGE_OFFER;
    }

    @Override
    public String getName() {
        return "PublishExchangeOffer";
    }

    @Override
    public MonetarySystemPublishExchangeOffer parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemPublishExchangeOffer(buffer);
    }

    @Override
    public MonetarySystemPublishExchangeOffer parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemPublishExchangeOffer(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemPublishExchangeOffer attachment = (MonetarySystemPublishExchangeOffer) transaction.getAttachment();
        if (attachment.getBuyRateATM() <= 0 || attachment.getSellRateATM() <= 0 || attachment.getBuyRateATM() > attachment.getSellRateATM()) {
            throw new AplException.NotValidException(String.format("Invalid exchange offer, buy rate %d and sell rate %d has to be larger than 0, buy rate cannot be larger than sell rate", attachment.getBuyRateATM(), attachment.getSellRateATM()));
        }
        if (attachment.getTotalBuyLimit() < 0 || attachment.getTotalSellLimit() < 0 || attachment.getInitialBuySupply() < 0 || attachment.getInitialSellSupply() < 0 || attachment.getExpirationHeight() < 0) {
            throw new AplException.NotValidException("Invalid exchange offer, units and height cannot be negative: " + attachment.getJSONObject());
        }
        if (attachment.getTotalBuyLimit() < attachment.getInitialBuySupply() || attachment.getTotalSellLimit() < attachment.getInitialSellSupply()) {
            throw new AplException.NotValidException("Initial supplies must not exceed total limits");
        }
        if (attachment.getTotalBuyLimit() == 0 && attachment.getTotalSellLimit() == 0) {
            throw new AplException.NotValidException("Total buy and sell limits cannot be both 0");
        }
        if (attachment.getInitialBuySupply() == 0 && attachment.getInitialSellSupply() == 0) {
            throw new AplException.NotValidException("Initial buy and sell supply cannot be both 0");
        }
        if (attachment.getExpirationHeight() <= attachment.getFinishValidationHeight(transaction)) {
            throw new AplException.NotCurrentlyValidException("Expiration height must be after transaction execution height");
        }
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        CurrencyType.validate(currency, transaction);
        if (!currency.isActive()) {
            throw new AplException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemPublishExchangeOffer attachment = (MonetarySystemPublishExchangeOffer) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM()) && senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getInitialSellSupply()) {
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM()));
            senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getInitialSellSupply());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemPublishExchangeOffer attachment = (MonetarySystemPublishExchangeOffer) transaction.getAttachment();
        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM()));
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getInitialSellSupply());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemPublishExchangeOffer attachment = (MonetarySystemPublishExchangeOffer) transaction.getAttachment();
        CurrencyExchangeOffer.publishOffer(transaction, attachment);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }
    
}
