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
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyBuyOfferService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencySellOfferService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CurrencyExchangeOfferFacadeImpl implements CurrencyExchangeOfferFacade {

    public static final DbClause availableOnlyDbClause = new DbClause.LongClause("unit_limit", DbClause.Op.NE, 0)
        .and(new DbClause.LongClause("supply", DbClause.Op.NE, 0));

    private final CurrencyBuyOfferService currencyBuyOfferService;
    private final CurrencySellOfferService currencySellOfferService;
    private final BlockChainInfoService blockChainInfoService;
    private final AccountService accountService;
    private final AccountCurrencyService accountCurrencyService;
    private final ExchangeService exchangeService;

    @Inject
    public CurrencyExchangeOfferFacadeImpl(CurrencyBuyOfferService currencyBuyOfferService,
                                           CurrencySellOfferService currencySellOfferService,
                                           BlockChainInfoService blockChainInfoService,
                                           AccountService accountService,
                                           AccountCurrencyService accountCurrencyService,
                                           ExchangeService exchangeService) {
        this.currencyBuyOfferService = currencyBuyOfferService;
        this.currencySellOfferService = currencySellOfferService;
        this.blockChainInfoService = blockChainInfoService;
        this.accountService = accountService;
        this.accountCurrencyService = accountCurrencyService;
        this.exchangeService = exchangeService;
    }

    @Override
    public void publishOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment) {
        CurrencyBuyOffer previousOffer = currencyBuyOfferService.getOffer(attachment.getCurrencyId(), transaction.getSenderId());
        if (previousOffer != null) {
            this.removeOffer(LedgerEvent.CURRENCY_OFFER_REPLACED, previousOffer);
        }
        currencyBuyOfferService.addOffer(transaction, attachment);
        currencySellOfferService.addOffer(transaction, attachment);
    }

    @Override
    public void removeOffer(LedgerEvent event, CurrencyBuyOffer buyOffer) {
        CurrencySellOffer sellOffer = currencySellOfferService.getOffer(buyOffer.getId());

        currencyBuyOfferService.remove(buyOffer);
        currencySellOfferService.remove(sellOffer);

        Account account = accountService.getAccount(buyOffer.getAccountId());
        accountService.addToUnconfirmedBalanceATM(account, event, buyOffer.getId(), Math.multiplyExact(buyOffer.getSupply(), buyOffer.getRateATM()));
        accountCurrencyService.addToUnconfirmedCurrencyUnits(account, event, buyOffer.getId(), buyOffer.getCurrencyId(), sellOffer.getSupply());
    }

    @Override
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

    @Override
    public AvailableOffers getAvailableToBuy(final long currencyId, final long units) {
        return calculateTotal(getAvailableSellOffers(currencyId, 0L), units);
    }

    @Override
    public List<CurrencyExchangeOffer> getAvailableSellOffers(long currencyId, long maxRateATM) {
        List<CurrencyExchangeOffer> currencySellOffers = new ArrayList<>();
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId).and(availableOnlyDbClause);
        if (maxRateATM > 0) {
            dbClause = dbClause.and(new DbClause.LongClause("rate", DbClause.Op.LTE, maxRateATM));
        }
        try (DbIterator<CurrencySellOffer> offers = currencySellOfferService.getOffers(dbClause, 0, -1,
            " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencySellOffer offer : offers) {
                currencySellOffers.add(offer);
            }
        }
        return currencySellOffers;
    }

    @Override
    public AvailableOffers getAvailableToSell(final long currencyId, final long units) {
        return calculateTotal(getAvailableBuyOffers(currencyId, 0L), units);
    }

    @Override
    public List<CurrencyExchangeOffer> getAvailableBuyOffers(long currencyId, long minRateATM) {
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

    @Override
    public void exchangeCurrencyForAPL(Transaction transaction, Account account, final long currencyId, final long rateATM, final long units) {
        List<CurrencyExchangeOffer> currencyBuyOffers = getAvailableBuyOffers(currencyId, rateATM);
        if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
            log.trace("account === 0 exchangeCurrencyForAPL account={}, currencyId={}, rateATM={}, units={}\nstack={}",
                account, currencyId, rateATM, units, ThreadUtils.last5Stacktrace());
            log.trace("account === 0 exchangeCurrencyForAPL accountCurrency={}",
                accountCurrencyService.getAccountCurrency(account.getId(), currencyId));
        }

        long totalAmountATM = 0;
        long remainingUnits = units;
        for (CurrencyExchangeOffer asBuyOffer : currencyBuyOffers) {
            if (remainingUnits == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingUnits, asBuyOffer.getSupply()), asBuyOffer.getLimit());
            long curAmountATM = Math.multiplyExact(curUnits, asBuyOffer.getRateATM());

            totalAmountATM = Math.addExact(totalAmountATM, curAmountATM);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);

            int height = blockChainInfoService.getHeight();
            asBuyOffer.decreaseLimitAndSupply(curUnits);
            asBuyOffer.setHeight(height);
            currencyBuyOfferService.insert((CurrencyBuyOffer)asBuyOffer); // store new buy values
            CurrencySellOffer sellOffer = currencySellOfferService.getOffer(asBuyOffer.getId()); // get contra-offer
            long excess = sellOffer.increaseSupply(curUnits); // change value
            sellOffer.setHeight(height);
            currencySellOfferService.insert(sellOffer); // store new sell values

            Account counterAccount = accountService.getAccount(sellOffer.getAccountId());
            if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
                log.trace("account === 1 exchangeCurrencyForAPL account={}", counterAccount);
            }
            accountService.addToBalanceATM(
                counterAccount, LedgerEvent.CURRENCY_EXCHANGE, asBuyOffer.getId(), -curAmountATM);
            accountCurrencyService.addToCurrencyUnits(
                counterAccount, LedgerEvent.CURRENCY_EXCHANGE, asBuyOffer.getId(), currencyId, curUnits);
            accountCurrencyService.addToUnconfirmedCurrencyUnits(
                counterAccount, LedgerEvent.CURRENCY_EXCHANGE, asBuyOffer.getId(), currencyId, excess);
            if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
                log.trace("account === 2 exchangeCurrencyForAPL account={}", counterAccount);
            }

            // update data in Exchange
            exchangeService.addExchange(transaction, currencyId, asBuyOffer.getId(), account.getId(),
                asBuyOffer.getAccountId(), curUnits, /*blockChainInfoService.getLastBlock(),*/ asBuyOffer.getRateATM());
        }
        long transactionId = transaction.getId();
        account = accountService.getAccount(account);
        if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
            log.trace("account === 3 exchangeCurrencyForAPL account={}", account);
        }
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, totalAmountATM);
        accountCurrencyService.addToCurrencyUnits(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, -(units - remainingUnits));
        accountCurrencyService.addToUnconfirmedCurrencyUnits(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, remainingUnits);
        if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
            log.trace("account === 4 exchangeCurrencyForAPL account={}", account);
            log.trace("account === 4 exchangeCurrencyForAPL accountCurrency={}",
                accountCurrencyService.getAccountCurrency(account.getId(), currencyId));
        }
    }

    @Override
    public void exchangeAPLForCurrency(Transaction transaction, Account account, final long currencyId, final long rateATM, final long units) {
        List<CurrencyExchangeOffer> currencySellOffers = getAvailableSellOffers(currencyId, rateATM);
        long totalAmountATM = 0;
        long remainingUnits = units;
        if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
            log.trace("account === 0 exchangeAPLForCurrency account={}, currencyId={}, rateATM={}, units={}\nstack={}",
                account, currencyId, rateATM, units, ThreadUtils.last5Stacktrace());
            log.trace("account === 0 exchangeAPLForCurrency accountCurrency={}",
                accountCurrencyService.getAccountCurrency(account.getId(), currencyId));
        }
        for (CurrencyExchangeOffer asSellOffer : currencySellOffers) {
            if (remainingUnits == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingUnits, asSellOffer.getSupply()), asSellOffer.getLimit());
            long curAmountATM = Math.multiplyExact(curUnits, asSellOffer.getRateATM());

            totalAmountATM = Math.addExact(totalAmountATM, curAmountATM);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);

            int height = blockChainInfoService.getHeight();
            asSellOffer.decreaseLimitAndSupply(curUnits);
            asSellOffer.setHeight(height);
            currencySellOfferService.insert((CurrencySellOffer)asSellOffer); // store new sell values
            CurrencyBuyOffer buyOffer = currencyBuyOfferService.getOffer(asSellOffer.getId()); // get contra-offer
            long excess = buyOffer.increaseSupply(curUnits); // change value
            buyOffer.setHeight(height);
            currencyBuyOfferService.insert(buyOffer); // store new buy values

            Account counterAccount = accountService.getAccount(buyOffer.getAccountId());
            if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
                log.trace("account === 1 exchangeAPLForCurrency account={}", counterAccount);
            }
            accountService.addToBalanceATM(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, asSellOffer.getId(), curAmountATM);
            accountService.addToUnconfirmedBalanceATM(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, asSellOffer.getId(),
                Math.addExact(
                    Math.multiplyExact(curUnits - excess, asSellOffer.getRateATM() - buyOffer.getRateATM()),
                    Math.multiplyExact(excess, asSellOffer.getRateATM())
                )
            );
            accountCurrencyService.addToCurrencyUnits(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, asSellOffer.getId(), currencyId, -curUnits);
            if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
                log.trace("account === 2 exchangeAPLForCurrency account={}", counterAccount);
            }
            exchangeService.addExchange(transaction, currencyId, asSellOffer.getId(), asSellOffer.getAccountId(),
                account.getId(), curUnits, /*blockChainInfoService.getLastBlock(),*/ asSellOffer.getRateATM());
        }
        long transactionId = transaction.getId();
        account = accountService.getAccount(account);
        if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
            log.trace("account === 3 exchangeAPLForCurrency account={}", account);
        }
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId,
            currencyId, Math.subtractExact(units, remainingUnits));
        accountService.addToBalanceATM(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, -totalAmountATM);
        accountService.addToUnconfirmedBalanceATM(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId,
            Math.multiplyExact(units, rateATM) - totalAmountATM);
        if (log.isTraceEnabled() /*&& (account.getId() == 2650055114867906720L || account.getId() == 5122426243196961555L)*/) {
            log.trace("account === 4 exchangeAPLForCurrency account={}", account);
            log.trace("account === 4 exchangeAPLForCurrency accountCurrency={}",
                accountCurrencyService.getAccountCurrency(account.getId(), currencyId));
        }
    }

    @Override
    public CurrencyBuyOfferService getCurrencyBuyOfferService() {
        return currencyBuyOfferService;
    }

    @Override
    public CurrencySellOfferService getCurrencySellOfferService() {
        return currencySellOfferService;
    }
}
