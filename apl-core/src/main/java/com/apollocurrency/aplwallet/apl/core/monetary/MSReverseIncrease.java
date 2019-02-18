/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveIncrease;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import javax.enterprise.inject.spi.CDI;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class MSReverseIncrease extends MonetarySystem {
    
    public MSReverseIncrease() {
    }

    @Override
    public byte getSubtype() {
        return SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_RESERVE_INCREASE;
    }

    @Override
    public String getName() {
        return "ReserveIncrease";
    }

    @Override
    public MonetarySystemReserveIncrease parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemReserveIncrease(buffer);
    }

    @Override
    public MonetarySystemReserveIncrease parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemReserveIncrease(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        if (attachment.getAmountPerUnitATM() <= 0) {
            throw new AplException.NotValidException("Reserve increase amount must be positive: " + attachment.getAmountPerUnitATM());
        }
        CurrencyType.validate(Currency.getCurrency(attachment.getCurrencyId()), transaction);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitATM())) {
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitATM()));
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        long reserveSupply;
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            reserveSupply = currency.getReserveSupply();
        } else {
            // currency must have been deleted, get reserve supply from the original issuance transaction
            Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
            Transaction currencyIssuance = blockchain.getTransaction(attachment.getCurrencyId());
            MonetarySystemCurrencyIssuance currencyIssuanceAttachment = (MonetarySystemCurrencyIssuance) currencyIssuance.getAttachment();
            reserveSupply = currencyIssuanceAttachment.getReserveSupply();
        }
        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), Math.multiplyExact(reserveSupply, attachment.getAmountPerUnitATM()));
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        Currency.increaseReserve(getLedgerEvent(), transaction.getId(), senderAccount, attachment.getCurrencyId(), attachment.getAmountPerUnitATM());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }
    
}
