/*
 * Copyright © 2018-2020 Apollo Foundation
*/

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.AvailableOffers;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.Exchange;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CurrencyExchangeOfferFacadeImpl {

    public static final DbClause availableOnlyDbClause = new DbClause.LongClause("unit_limit", DbClause.Op.NE, 0)
        .and(new DbClause.LongClause("supply", DbClause.Op.NE, 0));

    private final CurrencyBuyOfferServiceImpl currencyBuyOfferService;
    private final CurrencySellOfferServiceImpl currencySellOfferService;
    private final BlockChainInfoService blockChainInfoService;
    private final AccountService accountService;
    private final AccountCurrencyService accountCurrencyService;


    @Inject
    public CurrencyExchangeOfferFacadeImpl(CurrencyBuyOfferServiceImpl currencyBuyOfferService,
                                           CurrencySellOfferServiceImpl currencySellOfferService,
                                           BlockChainInfoService blockChainInfoService,
                                           AccountService accountService,
                                           AccountCurrencyService accountCurrencyService) {
        this.currencyBuyOfferService = currencyBuyOfferService;
        this.currencySellOfferService = currencySellOfferService;
        this.blockChainInfoService = blockChainInfoService;
        this.accountService = accountService;
        this.accountCurrencyService = accountCurrencyService;
    }

    public void publishOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment) {
        CurrencyBuyOffer previousOffer = currencyBuyOfferService.getOffer(attachment.getCurrencyId(), transaction.getSenderId());
        if (previousOffer != null) {
            this.removeOffer(LedgerEvent.CURRENCY_OFFER_REPLACED, previousOffer);
        }
        currencyBuyOfferService.addOffer(transaction, attachment);
        currencySellOfferService.addOffer(transaction, attachment);
    }

    public void removeOffer(LedgerEvent event, CurrencyBuyOffer buyOffer) {
//        CurrencySellOffer sellOffer =  buyOffer.getCounterOffer();
        CurrencySellOffer sellOffer = currencySellOfferService.getOffer(buyOffer.getId());

        currencyBuyOfferService.remove(buyOffer);
        currencySellOfferService.remove(sellOffer);

        Account account = accountService.getAccount(buyOffer.getAccountId());
        accountService.addToUnconfirmedBalanceATM(account, event, buyOffer.getId(), Math.multiplyExact(buyOffer.getSupply(), buyOffer.getRateATM()));
        accountCurrencyService.addToUnconfirmedCurrencyUnits(account, event, buyOffer.getId(), buyOffer.getCurrencyId(), sellOffer.getSupply());
    }

    public AvailableOffers calculateTotal(List<CurrencyExchangeOffer> offers, final long units) {
        long totalAmountATM = 0;
        long remainingUnits = units;
        long rateATM = 0;
        for (CurrencyExchangeOffer offer : offers) {
            if (remainingUnits == 0) {
                break;
            }
            rateATM = offer.getRateATM();
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountATM = Math.multiplyExact(curUnits, offer.getRateATM());
            totalAmountATM = Math.addExact(totalAmountATM, curAmountATM);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);
        }
        return new AvailableOffers(rateATM, Math.subtractExact(units, remainingUnits), totalAmountATM);
    }

    AvailableOffers getAvailableToSell(final long currencyId, final long units) {
        return calculateTotal(getAvailableBuyOffers(currencyId, 0L), units);
    }

    List<CurrencyExchangeOffer> getAvailableBuyOffers(long currencyId, long minRateATM) {
        List<CurrencyExchangeOffer> currencyExchangeOffers = new ArrayList<>();
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId).and(availableOnlyDbClause);
        if (minRateATM > 0) {
            dbClause = dbClause.and(new DbClause.LongClause("rate", DbClause.Op.GTE, minRateATM));
        }
        try (DbIterator<CurrencyBuyOffer> offers = currencyBuyOfferService.getOffers(dbClause, 0, -1,
            " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencyBuyOffer offer : offers) {
                currencyExchangeOffers.add(offer);
            }
        }
        return currencyExchangeOffers;
    }

    public void exchangeCurrencyForAPL(Transaction transaction, Account account, final long currencyId, final long rateATM, final long units) {
        List<CurrencyExchangeOffer> currencyBuyOffers = getAvailableBuyOffers(currencyId, rateATM);
        log.trace("account === 0 exchangeCurrencyForAPL account={}, currencyId={}, rateATM={}, units={}", account, currencyId, rateATM, units);
        long totalAmountATM = 0;
        long remainingUnits = units;
        for (CurrencyExchangeOffer offer : currencyBuyOffers) {
            if (remainingUnits == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountATM = Math.multiplyExact(curUnits, offer.getRateATM());

            totalAmountATM = Math.addExact(totalAmountATM, curAmountATM);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);

//            offer.decreaseLimitAndSupply(curUnits); //TODO: YL - fix that
//            long excess = offer.getCounterOffer().increaseSupply(curUnits);
            long excess = 0; // prevent compile error

            changeAccountCurrencyBalances(currencyId, curUnits, curAmountATM, excess,
                offer.getAccountId(), offer.getId());
            // TODO: YL - fix by replacing ExchangeService
            Exchange.addExchange(transaction, currencyId, offer.getId(), account.getId(),
                offer.getAccountId(), curUnits, blockChainInfoService.getLastBlock(), offer.getRateATM());
        }
        long transactionId = transaction.getId();
        account = accountService.getAccount(account);
        log.trace("account === 3 exchangeCurrencyForAPL account={}", account);
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, totalAmountATM);
        accountCurrencyService.addToCurrencyUnits(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, -(units - remainingUnits));
        accountCurrencyService.addToUnconfirmedCurrencyUnits(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, remainingUnits);
        log.trace("account === 4 exchangeCurrencyForAPL account={}", account);
    }

    public void changeAccountCurrencyBalances(long currencyId, long curUnits, long curAmountATM, long excess,
                                              long accountId, long id) {
        Account counterAccount = accountService.getAccount(accountId);
        log.trace("account === 1 exchangeCurrencyForAPL account={}", counterAccount);
        accountService.addToBalanceATM(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, id, -curAmountATM);
        accountCurrencyService.addToCurrencyUnits(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, id, currencyId, curUnits);
        accountCurrencyService.addToUnconfirmedCurrencyUnits(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, id, currencyId, excess);
        log.trace("account === 2 exchangeCurrencyForAPL account={}", counterAccount);
    }

/*
    long increaseSupply(long delta) {
        long excess = Math.max(Math.addExact(supply, Math.subtractExact(delta, limit)), 0);
        supply += delta - excess;
        return excess;
    }

    void decreaseLimitAndSupply(long delta) {
        limit -= delta;
        supply -= delta;
    }
*/
}
