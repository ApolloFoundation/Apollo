/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyTransferService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class MSCurrencyTransferTransactionType extends MonetarySystemTransactionType {
    private final AccountCurrencyService accountCurrencyService;
    private final CurrencyTransferService accountTransferService;

    @Inject
    public MSCurrencyTransferTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, AccountCurrencyService accountCurrencyService, CurrencyTransferService accountTransferService) {
        super(blockchainConfig, accountService, currencyService);
        this.accountCurrencyService = accountCurrencyService;
        this.accountTransferService = accountTransferService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_CURRENCY_TRANSFER;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
        if (!currencyService.isActive(currency)) {
            throw new AplException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        if (attachment.getUnits() <= 0) {
            throw new AplException.NotValidException("Invalid currency transfer: " + attachment.getJSONObject());
        }
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Currency transfer to genesis account not allowed");
        }

    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        if (attachment.getUnits() > accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, attachment.getCurrencyId())) {
            return false;
        }
        accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemCurrencyTransfer attachment = (MonetarySystemCurrencyTransfer) transaction.getAttachment();
        currencyService.transferCurrency(getLedgerEvent(), transaction.getId(), senderAccount, recipientAccount, attachment.getCurrencyId(), attachment.getUnits());
        accountTransferService.addTransfer(transaction, attachment);
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

}
