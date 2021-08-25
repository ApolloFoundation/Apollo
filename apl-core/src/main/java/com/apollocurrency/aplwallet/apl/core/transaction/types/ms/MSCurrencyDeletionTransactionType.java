/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyDeletion;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.MS_CURRENCY_DELETION;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE;

@Singleton
public class MSCurrencyDeletionTransactionType extends MSTransactionType {

    @Inject
    public MSCurrencyDeletionTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService) {
        super(blockchainConfig, accountService, currencyService);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return MS_CURRENCY_DELETION;
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
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MonetarySystemCurrencyDeletion attachment = (MonetarySystemCurrencyDeletion) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        String nameLower = currency.getName().toLowerCase();
        String codeLower = currency.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(MS_CURRENCY_ISSUANCE, nameLower, duplicates, true);
        if (!nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(MS_CURRENCY_ISSUANCE, codeLower, duplicates, true);
        }
        return isDuplicate;
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyDeletion attachment = (MonetarySystemCurrencyDeletion) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
        if (!currencyService.canBeDeletedBy(currency, transaction.getSenderId())) {
            throw new AplException.NotCurrentlyValidException(
                "Currency " + currency.getId() + " cannot be deleted by account " + transaction.getSenderId());
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {

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
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.delete(currency, getLedgerEvent(), transaction.getId(), senderAccount);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
