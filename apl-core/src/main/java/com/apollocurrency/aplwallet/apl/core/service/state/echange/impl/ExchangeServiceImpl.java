/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.echange.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ExchangeServiceImpl {

    private final ExchangeTable exchangeTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<Exchange> exchangeIteratorToStreamConverter;

    @Inject
    public ExchangeServiceImpl(ExchangeTable exchangeTable, BlockChainInfoService blockChainInfoService) {
        this.exchangeTable = exchangeTable;
        this.blockChainInfoService = blockChainInfoService;
        this.exchangeIteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    public Exchange addExchange(Transaction transaction, long currencyId, CurrencyExchangeOffer offer,
                                long sellerId, long buyerId, long units,
                                Block lastBlock) {
        Block block = blockChainInfoService.getLastBlock();
        Exchange exchange = new Exchange(
            transaction.getId(), currencyId, lastBlock.getId(), offer.getId(), sellerId, buyerId, units,
            offer.getRateATM(), block.getTimestamp(), block.getHeight());
        exchangeTable.insert(exchange);
        return exchange;
    }

    public Exchange addExchange(Transaction transaction, long currencyId,
                                       long offerId,
                                       long sellerId, long buyerId, long units,
                                       Block lastBlock,
                                       long rateATM) {
        Block block = blockChainInfoService.getLastBlock();
        Exchange exchange = new Exchange(
            transaction.getId(), currencyId, lastBlock.getId(), offerId, sellerId, buyerId, units,
            rateATM, block.getTimestamp(), block.getHeight());
        exchangeTable.insert(exchange);
        return exchange;
    }


    public DbIterator<Exchange> getAllExchanges(int from, int to) {
        return exchangeTable.getAll(from, to);
    }

    public int getCount() {
        return exchangeTable.getCount();
    }

    public DbIterator<Exchange> getCurrencyExchanges(long currencyId, int from, int to) {
        return exchangeTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    public List<Exchange> getLastExchanges(long[] currencyIds) {
        return exchangeTable.getLastExchanges(currencyIds);
    }

    public DbIterator<Exchange> getAccountExchanges(long accountId, int from, int to) {
        return exchangeTable.getAccountExchanges(accountId, from, to);
    }

    public DbIterator<Exchange> getAccountCurrencyExchanges(long accountId, long currencyId, int from, int to) {
        return exchangeTable.getAccountCurrencyExchanges(accountId, currencyId, from, to);
    }

    public DbIterator<Exchange> getExchanges(long transactionId) {
        return exchangeTable.getManyBy(new DbClause.LongClause("transaction_id", transactionId), 0, -1);
    }

    public DbIterator<Exchange> getOfferExchanges(long offerId, int from, int to) {
        return exchangeTable.getManyBy(new DbClause.LongClause("offer_id", offerId), from, to);
    }

    public int getExchangeCount(long currencyId) {
        return exchangeTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

}
