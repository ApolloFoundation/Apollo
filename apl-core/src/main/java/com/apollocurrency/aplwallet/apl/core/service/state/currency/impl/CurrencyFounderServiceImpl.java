/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyFounderTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CurrencyFounderServiceImpl implements com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyFounderService {

    private final CurrencyFounderTable currencyFounderTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<CurrencyFounder> iteratorToStreamConverter;

    @Inject
    public CurrencyFounderServiceImpl(CurrencyFounderTable currencyFounderTable,
                                      BlockChainInfoService blockChainInfoService) {
        this.currencyFounderTable = currencyFounderTable;
        this.blockChainInfoService = blockChainInfoService;
        this.iteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * for unit test
     */
    public CurrencyFounderServiceImpl(CurrencyFounderTable currencyFounderTable,
                                      BlockChainInfoService blockChainInfoService,
                                      IteratorToStreamConverter<CurrencyFounder> iteratorToStreamConverter) {
        this.currencyFounderTable = currencyFounderTable;
        this.blockChainInfoService = blockChainInfoService;
        this.iteratorToStreamConverter = iteratorToStreamConverter != null ?
            iteratorToStreamConverter : new IteratorToStreamConverter<>();
    }

    @Override
    public void addOrUpdateFounder(long currencyId, long accountId, long amount) {
        CurrencyFounder founder = getFounder(currencyId, accountId);
        int height = blockChainInfoService.getHeight();
        if (founder == null) {
            founder = new CurrencyFounder(currencyId, accountId, amount, height);
        } else {
            long amountPerUnitATM = founder.getAmountPerUnitATM();
            amountPerUnitATM += amount;
            founder.setAmountPerUnitATM(amountPerUnitATM);
            founder.setHeight(height); // important height assign
        }
        currencyFounderTable.insert(founder);
    }

    @Override
    public CurrencyFounder getFounder(long currencyId, long accountId) {
        return currencyFounderTable.get(CurrencyFounderTable.currencyFounderDbKeyFactory.newKey(currencyId, accountId));
    }

    @Override
    public DbIterator<CurrencyFounder> getCurrencyFounders(long currencyId, int from, int to) {
        return currencyFounderTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    @Override
    public Stream<CurrencyFounder> getCurrencyFoundersStream(long currencyId, int from, int to) {
        return iteratorToStreamConverter.apply(
            currencyFounderTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to)
        );
    }

    @Override
    public DbIterator<CurrencyFounder> getFounderCurrencies(long accountId, int from, int to) {
        return currencyFounderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    @Override
    public Stream<CurrencyFounder> getFounderCurrenciesStream(long accountId, int from, int to) {
        return iteratorToStreamConverter.apply(
            currencyFounderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to)
        );
    }

    @Override
    public void remove(long currencyId) {
        List<CurrencyFounder> founders = new ArrayList<>();
        try (DbIterator<CurrencyFounder> currencyFounders = this.getCurrencyFounders(currencyId, 0, Integer.MAX_VALUE)) {
            for (CurrencyFounder founder : currencyFounders) {
                founders.add(founder);
            }
        }
        int height = blockChainInfoService.getHeight();
        founders.forEach(f -> {
            f.setHeight(height);// important height assign
            currencyFounderTable.deleteAtHeight(f, blockChainInfoService.getHeight());
        });
    }


}
