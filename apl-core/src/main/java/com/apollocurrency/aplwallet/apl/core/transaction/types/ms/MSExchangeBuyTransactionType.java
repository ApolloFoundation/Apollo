/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeRequestService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class MSExchangeBuyTransactionType extends MonetarySystemExchangeTransactionType {
    private final ExchangeRequestService exchangeRequestService;
    private final CurrencyExchangeOfferFacade currencyExchangeOfferFacade;

    @Inject
    public MSExchangeBuyTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, ExchangeRequestService exchangeRequestService, CurrencyExchangeOfferFacade currencyExchangeOfferFacade) {
        super(blockchainConfig, accountService, currencyService);
        this.exchangeRequestService = exchangeRequestService;
        this.currencyExchangeOfferFacade = currencyExchangeOfferFacade;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_EXCHANGE_BUY;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_EXCHANGE_BUY;
    }

    @Override
    public String getName() {
        return "ExchangeBuy";
    }

    @Override
    public MonetarySystemExchangeBuyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemExchangeBuyAttachment(buffer);
    }

    @Override
    public MonetarySystemExchangeBuyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemExchangeBuyAttachment(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return super.isDuplicate(transaction, duplicates);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemExchangeBuyAttachment attachment = (MonetarySystemExchangeBuyAttachment) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getUnits(), attachment.getRateATM())) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -Math.multiplyExact(attachment.getUnits(), attachment.getRateATM()));
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MonetarySystemExchangeBuyAttachment attachment = (MonetarySystemExchangeBuyAttachment) transaction.getAttachment();
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getUnits(), attachment.getRateATM()));
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemExchangeBuyAttachment attachment = (MonetarySystemExchangeBuyAttachment) transaction.getAttachment();
        exchangeRequestService.addExchangeRequest(transaction, attachment);
        currencyExchangeOfferFacade.exchangeAPLForCurrency(transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateATM(), attachment.getUnits());
    }

}
