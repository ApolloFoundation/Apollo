/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuanceAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveIncreaseAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class MSReserveIncreaseTransactionType extends MSTransactionType {
    private final Blockchain blockchain;

    @Inject
    public MSReserveIncreaseTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, Blockchain blockchain) {
        super(blockchainConfig, accountService, currencyService);
        this.blockchain = blockchain;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_RESERVE_INCREASE;
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
    public MonetarySystemReserveIncreaseAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemReserveIncreaseAttachment(buffer);
    }

    @Override
    public MonetarySystemReserveIncreaseAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemReserveIncreaseAttachment(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemReserveIncreaseAttachment attachment = (MonetarySystemReserveIncreaseAttachment) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
        long reservedATMs = Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitATM());
        verifyAccountBalanceSufficiency(transaction, reservedATMs);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemReserveIncreaseAttachment attachment = (MonetarySystemReserveIncreaseAttachment) transaction.getAttachment();
        if (attachment.getAmountPerUnitATM() <= 0) {
            throw new AplException.NotValidException("Reserve increase amount must be positive: " + attachment.getAmountPerUnitATM());
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveIncreaseAttachment attachment = (MonetarySystemReserveIncreaseAttachment) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        long reservedATMs = Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitATM());
        if (senderAccount.getUnconfirmedBalanceATM() >= reservedATMs) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -reservedATMs);
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveIncreaseAttachment attachment = (MonetarySystemReserveIncreaseAttachment) transaction.getAttachment();
        long reserveSupply;
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            reserveSupply = currency.getReserveSupply();
        } else {
            // May occur when currency was deleted, but reserveIncrease transaction was phased earlier and didn't pass
            // validation, so that should be reverted
            Transaction currencyIssuance = blockchain.getTransaction(attachment.getCurrencyId());
            MonetarySystemCurrencyIssuanceAttachment currencyIssuanceAttachment = (MonetarySystemCurrencyIssuanceAttachment) currencyIssuance.getAttachment();
            reserveSupply = currencyIssuanceAttachment.getReserveSupply();
        }
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(reserveSupply, attachment.getAmountPerUnitATM()));
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemReserveIncreaseAttachment attachment = (MonetarySystemReserveIncreaseAttachment) transaction.getAttachment();
        currencyService.increaseReserve(getLedgerEvent(), transaction.getId(), senderAccount, attachment.getCurrencyId(), attachment.getAmountPerUnitATM());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
