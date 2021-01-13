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
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveClaim;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class MSReserveClaimTransactionType extends MonetarySystemTransactionType {
    private final AccountCurrencyService accountCurrencyService;

    @Inject
    public MSReserveClaimTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, AccountCurrencyService accountCurrencyService) {
        super(blockchainConfig, accountService, currencyService);
        this.accountCurrencyService = accountCurrencyService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_RESERVE_CLAIM;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        if (attachment.getUnits() <= 0) {
            throw new AplException.NotValidException("Reserve claim number of units must be positive: " + attachment.getUnits());
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        if (accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, attachment.getCurrencyId()) >= attachment.getUnits()) {
            accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemReserveClaim attachment = (MonetarySystemReserveClaim) transaction.getAttachment();
        currencyService.claimReserve(getLedgerEvent(), transaction.getId(), senderAccount, attachment.getCurrencyId(), attachment.getUnits());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
