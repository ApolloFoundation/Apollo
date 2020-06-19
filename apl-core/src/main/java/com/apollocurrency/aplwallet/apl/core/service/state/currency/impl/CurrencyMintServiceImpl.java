/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.mint.CurrencyMinting;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyMintTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyMintService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CurrencyMintServiceImpl implements CurrencyMintService {

    private final CurrencyMintTable currencyMintTable;
    private final BlockChainInfoService blockChainInfoService;
    private final AccountCurrencyService accountCurrencyService;
    private final CurrencyService currencyService;

    @Inject
    public CurrencyMintServiceImpl(CurrencyMintTable currencyMintTable,
                                   BlockChainInfoService blockChainInfoService,
                                   AccountCurrencyService accountCurrencyService,
                                   CurrencyService currencyService) {
        this.currencyMintTable = currencyMintTable;
        this.blockChainInfoService = blockChainInfoService;
        this.accountCurrencyService = accountCurrencyService;
        this.currencyService = currencyService;
    }

    @Override
    public void mintCurrency(LedgerEvent event, long eventId, final Account account,
                             final MonetarySystemCurrencyMinting attachment) {
        CurrencyMint currencyMint = currencyMintTable.get(
            CurrencyMintTable.currencyMintDbKeyFactory.newKey(attachment.getCurrencyId(), account.getId()));
        if (currencyMint != null && attachment.getCounter() <= currencyMint.getCounter()) {
            return;
        }
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        if (CurrencyMinting.meetsTarget(account.getId(), currency, attachment)) {
            if (currencyMint == null) {
                currencyMint = new CurrencyMint(attachment.getCurrencyId(),
                    account.getId(), attachment.getCounter(), blockChainInfoService.getHeight());
            } else {
                currencyMint.setHeight( blockChainInfoService.getHeight() );// important assign
                currencyMint.setCounter( attachment.getCounter() );
            }
            currencyMintTable.insert(currencyMint);
            long units = Math.min(attachment.getUnits(),
                currency.getMaxSupply() - currency.getCurrencySupply().getCurrentSupply());
            accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
                account, event, eventId, currency.getCurrencyId(), units);
            currencyService.increaseSupply(currency, units);
        } else {
            log.debug("Currency mint hash no longer meets target %s", attachment.getJSONObject().toJSONString());
        }
    }

    @Override
    public long getCounter(long currencyId, long accountId) {
        CurrencyMint currencyMint = currencyMintTable.get(CurrencyMintTable.currencyMintDbKeyFactory.newKey(currencyId, accountId));
        if (currencyMint != null) {
            return currencyMint.getCounter();
        } else {
            return 0;
        }
    }

    @Override
    public void deleteCurrency(Currency currency) {
        List<CurrencyMint> currencyMints = new ArrayList<>();
        try (DbIterator<CurrencyMint> mints = currencyMintTable.getManyBy(
            new DbClause.LongClause("currency_id", currency.getCurrencyId()), 0, -1)) {
            while (mints.hasNext()) {
                currencyMints.add(mints.next());
            }
        }
        int currentHeight = blockChainInfoService.getHeight();
        currencyMints.forEach(c -> {
            c.setHeight(currentHeight); // important assign
            currencyMintTable.deleteAtHeight(c, currentHeight);
        });
    }

}
