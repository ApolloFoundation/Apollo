/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyBurningAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class MSCurrencyBurningTransactionType extends MSTransactionType {
    private final AccountCurrencyService accountCurrencyService;
    @Inject
    public MSCurrencyBurningTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, AccountCurrencyService accountCurrencyService) {
        super(blockchainConfig, accountService, currencyService);
        this.accountCurrencyService = accountCurrencyService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_CURRENCY_BURNING;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_BURNING;
    }

    @Override
    public String getName() {
        return "CurrencyBurning";
    }

    @Override
    public MonetarySystemCurrencyBurningAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemCurrencyBurningAttachment(buffer);
    }

    @Override
    public MonetarySystemCurrencyBurningAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemCurrencyBurningAttachment(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyBurningAttachment attachment = (MonetarySystemCurrencyBurningAttachment) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
        if (!currencyService.isActive(currency)) {
            throw new AplException.NotCurrentlyValidException("Currency not currently active, unable to burn: " + attachment.getJSONObject());
        }
        long accountCurrencyBalance = accountCurrencyService.getUnconfirmedCurrencyUnits(transaction.getSenderId(), attachment.getCurrencyId());
        if (accountCurrencyBalance < attachment.getUnits()) {
            throw new AplException.NotCurrentlyValidException("Sender " + Long.toUnsignedString(transaction.getSenderId())
                + " has not enough  " +Long.toUnsignedString(attachment.getCurrencyId())
                + " currency to burn: required " + attachment.getUnits() + ", but has only " + accountCurrencyBalance);
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyBurningAttachment attachment = (MonetarySystemCurrencyBurningAttachment) transaction.getAttachment();
        if (attachment.getUnits() <= 0) {
            throw new AplException.NotValidException("Positive number of units should be specified for the currency burn, got: " + attachment.getJSONObject());
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemCurrencyBurningAttachment attachment = (MonetarySystemCurrencyBurningAttachment) transaction.getAttachment();
        if (attachment.getUnits() > accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, attachment.getCurrencyId())) {
            return false;
        }
        accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemCurrencyBurningAttachment attachment = (MonetarySystemCurrencyBurningAttachment) transaction.getAttachment();
        accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemCurrencyBurningAttachment attachment = (MonetarySystemCurrencyBurningAttachment) transaction.getAttachment();
        currencyService.burn(attachment.getCurrencyId(), senderAccount, attachment.getUnits(), transaction.getId());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }


}
