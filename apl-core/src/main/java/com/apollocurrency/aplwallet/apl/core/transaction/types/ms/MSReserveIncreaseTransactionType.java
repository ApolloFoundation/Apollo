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
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveIncrease;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class MSReserveIncreaseTransactionType extends MonetarySystemTransactionType {
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
    public MonetarySystemReserveIncrease parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemReserveIncrease(buffer);
    }

    @Override
    public MonetarySystemReserveIncrease parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemReserveIncrease(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        if (attachment.getAmountPerUnitATM() <= 0) {
            throw new AplException.NotValidException("Reserve increase amount must be positive: " + attachment.getAmountPerUnitATM());
        }
    }
// TODO: Check if exception will not be more suitable
    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        try{
            if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitATM())) {
                getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitATM()));
                return true;
            }
        return false;
        }
        catch (java.lang.ArithmeticException e)
        {
            log.error(e.getMessage());
            log.error("Error: attachment = {}", attachment);
            return false;
        }

    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        long reserveSupply;
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            reserveSupply = currency.getReserveSupply();
        } else {
            // TODO: find better solution, maybe extend this attachment and add currency reserve supply
            // can occur, when new block applied transaction which deleted currency, but this transaction was not confirmed and we should restore unconfirmed balance
            // So that we get reserve supply from the original issuance transaction
            Transaction currencyIssuance = blockchain.getTransaction(attachment.getCurrencyId());
            MonetarySystemCurrencyIssuance currencyIssuanceAttachment = (MonetarySystemCurrencyIssuance) currencyIssuance.getAttachment();
            reserveSupply = currencyIssuanceAttachment.getReserveSupply();
        }
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(reserveSupply, attachment.getAmountPerUnitATM()));
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemReserveIncrease attachment = (MonetarySystemReserveIncrease) transaction.getAttachment();
        currencyService.increaseReserve(getLedgerEvent(), transaction.getId(), senderAccount, attachment.getCurrencyId(), attachment.getAmountPerUnitATM());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
