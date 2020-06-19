/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.observer;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyFounderService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CrowdFundingObserver {

    private final AccountService accountService;
    private final AccountCurrencyService accountCurrencyService;
//    private final CurrencyExchangeOfferFacade currencyExchangeOfferFacade;
    private final CurrencyService currencyService;
    private final CurrencyTable currencyTable;
    private final CurrencyFounderService currencyFounderService;
    private final BlockChainInfoService blockChainInfoService;
    private final CurrencySupplyTable currencySupplyTable;

    @Inject
    public CrowdFundingObserver(AccountService accountService,
                                AccountCurrencyService accountCurrencyService,
//                                CurrencyExchangeOfferFacade currencyExchangeOfferFacade,
                                CurrencyService currencyService,
                                CurrencyTable currencyTable,
                                CurrencyFounderService currencyFounderService,
                                BlockChainInfoService blockChainInfoService,
                                CurrencySupplyTable currencySupplyTable) {
        this.accountService = accountService;
        this.accountCurrencyService = accountCurrencyService;
//        this.currencyExchangeOfferFacade = currencyExchangeOfferFacade;
        this.currencyService = currencyService;
        this.currencyTable = currencyTable;
        this.currencyFounderService = currencyFounderService;
        this.blockChainInfoService = blockChainInfoService;
        this.currencySupplyTable = currencySupplyTable;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        log.trace(":accept:CrowdFundingListener: START onBlockApplied", block.getHeight());
        try (DbIterator<Currency> issuedCurrencies = currencyTable.getManyBy(new DbClause.IntClause("issuance_height", block.getHeight()), 0, -1)) {
            for (Currency currency : issuedCurrencies) {
                if (currencyService.getCurrentReservePerUnitATM(currency) < currency.getMinReservePerUnitATM()) {
//                    listeners.notify(currency, Currency.Event.BEFORE_UNDO_CROWDFUNDING);
                    undoCrowdFunding(currency);
                } else {
//                    listeners.notify(currency, Currency.Event.BEFORE_DISTRIBUTE_CROWDFUNDING);
                    distributeCurrency(currency);
                }
            }
        }
        log.trace(":accept:CrowdFundingListener: END onBlockApplaid AFTER_BLOCK_APPLY. block={}", block.getHeight());
    }

    private void undoCrowdFunding(Currency currency) {
        try (DbIterator<CurrencyFounder> founders =
                 currencyFounderService.getCurrencyFounders(currency.getCurrencyId(), 0, Integer.MAX_VALUE)) {
            for (CurrencyFounder founder : founders) {
                accountService.addToBalanceAndUnconfirmedBalanceATM(
                    accountService.getAccount(founder.getAccountId()),
                    LedgerEvent.CURRENCY_UNDO_CROWDFUNDING,
                    currency.getCurrencyId(),
                    Math.multiplyExact(currency.getReserveSupply(),
                        founder.getAmountPerUnitATM()));
            }
        }
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
            accountService.getAccount(currency.getAccountId()),
            LedgerEvent.CURRENCY_UNDO_CROWDFUNDING, currency.getCurrencyId(),
            currency.getCurrencyId(), -currency.getInitialSupply());
        int height = blockChainInfoService.getHeight();
        currency.setHeight(height);
        currencyTable.deleteAtHeight(currency, height);
        currencyFounderService.remove(currency.getCurrencyId());
    }

    private void distributeCurrency(Currency currency) {
        long totalAmountPerUnit = 0;
        final long remainingSupply = currency.getReserveSupply() - currency.getInitialSupply();
        List<CurrencyFounder> currencyFounders = new ArrayList<>();
        try (DbIterator<CurrencyFounder> founders = currencyFounderService
            .getCurrencyFounders(currency.getCurrencyId(), 0, Integer.MAX_VALUE)) {
            for (CurrencyFounder founder : founders) {
                totalAmountPerUnit += founder.getAmountPerUnitATM();
                currencyFounders.add(founder);
            }
        }
        CurrencySupply currencySupply = currencyService.getSupplyDataByCurrency(currency);
        for (CurrencyFounder founder : currencyFounders) {
            long units = Math.multiplyExact(remainingSupply, founder.getAmountPerUnitATM()) / totalAmountPerUnit;
            long value = currencySupply.getCurrentSupply() + units;
            currencySupply.setCurrentSupply(value);
            accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
                accountService.getAccount(founder.getAccountId()),
                LedgerEvent.CURRENCY_DISTRIBUTION, currency.getCurrencyId(),
                currency.getCurrencyId(), units);
        }
        Account issuerAccount = accountService.getAccount(currency.getAccountId());
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
            issuerAccount, LedgerEvent.CURRENCY_DISTRIBUTION, currency.getCurrencyId(),
            currency.getCurrencyId(),
            currency.getReserveSupply() - currency.getCurrencySupply().getCurrentSupply());
        if (!currency.is(CurrencyType.CLAIMABLE)) {
            accountService.addToBalanceAndUnconfirmedBalanceATM(
                issuerAccount, LedgerEvent.CURRENCY_DISTRIBUTION, currency.getAccountId(),
                Math.multiplyExact(totalAmountPerUnit, currency.getReserveSupply()));
        }
        currencySupply.setCurrentSupply(currency.getReserveSupply());
        currencySupply.setHeight(blockChainInfoService.getHeight());
        currencySupplyTable.insert(currencySupply);
    }

}
