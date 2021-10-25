/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.MathUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class MSPublishExchangeOfferTransactionType extends MSTransactionType {
    private final AccountCurrencyService accountCurrencyService;
    private final CurrencyExchangeOfferFacade exchangeOfferFacade;
    private final TransactionValidator transactionValidator;

    @Inject
    public MSPublishExchangeOfferTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, AccountCurrencyService accountCurrencyService, CurrencyExchangeOfferFacade exchangeOfferFacade, TransactionValidator transactionValidator) {
        super(blockchainConfig, accountService, currencyService);
        this.accountCurrencyService = accountCurrencyService;
        this.exchangeOfferFacade = exchangeOfferFacade;
        this.transactionValidator = transactionValidator;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_PUBLISH_EXCHANGE_OFFER;
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
    public MSPublishExchangeOfferAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MSPublishExchangeOfferAttachment(buffer);
    }

    @Override
    public MSPublishExchangeOfferAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MSPublishExchangeOfferAttachment(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
        if (!currencyService.isActive(currency)) {
            throw new AplException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
        }
        verifyAccountBalanceSufficiency(transaction, Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM()));
        long accountCurrencyBalance = accountCurrencyService.getUnconfirmedCurrencyUnits(transaction.getSenderId(), attachment.getCurrencyId());
        if (accountCurrencyBalance < attachment.getInitialSellSupply()) {
            throw new AplException.NotCurrentlyValidException("Account " + Long.toUnsignedString(transaction.getSenderId())
                + " has not enough " + Long.toUnsignedString(attachment.getCurrencyId()) + " currency to publish currency " +
                " exchange offer: required " + attachment.getInitialSellSupply() + ", but has only " + accountCurrencyBalance);
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
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
        if (attachment.getExpirationHeight() <= transactionValidator.getFinishValidationHeight(transaction, attachment)) {
            throw new AplException.NotCurrentlyValidException("Expiration height must be after transaction execution height");
        }
        MathUtils.safeMultiply(attachment.getInitialBuySupply(), attachment.getBuyRateATM(), transaction);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM())
            && accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, attachment.getCurrencyId()) >= attachment.getInitialSellSupply()) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(),
                -Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM()));
            accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(),
                attachment.getCurrencyId(), -attachment.getInitialSellSupply());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM()));
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getInitialSellSupply());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
        exchangeOfferFacade.publishOffer(transaction, attachment);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
