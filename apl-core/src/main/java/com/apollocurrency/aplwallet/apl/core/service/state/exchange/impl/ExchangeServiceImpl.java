/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.exchange.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class ExchangeServiceImpl implements ExchangeService {

    private final ExchangeTable exchangeTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<Exchange> exchangeIteratorToStreamConverter;

    @Inject
    public ExchangeServiceImpl(ExchangeTable exchangeTable, BlockChainInfoService blockChainInfoService) {
        this.exchangeTable = exchangeTable;
        this.blockChainInfoService = blockChainInfoService;
        this.exchangeIteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * for unit tests
     */
    public ExchangeServiceImpl(ExchangeTable exchangeTable,
                               BlockChainInfoService blockChainInfoService,
                               IteratorToStreamConverter<Exchange> exchangeIteratorToStreamConverter) {
        this.exchangeTable = exchangeTable;
        this.blockChainInfoService = blockChainInfoService;
        if (exchangeIteratorToStreamConverter != null) {
            this.exchangeIteratorToStreamConverter = exchangeIteratorToStreamConverter;
        } else {
            this.exchangeIteratorToStreamConverter = new IteratorToStreamConverter<>();
        }
    }

    @Override
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

    @Override
    public Exchange addExchange(Transaction transaction, long currencyId,
                                long offerId,
                                long sellerId, long buyerId, long units,
//                                Block lastBlock,
                                long rateATM) {
        Block lastBlock = blockChainInfoService.getLastBlock();
        Exchange exchange = new Exchange(
            transaction.getId(), currencyId, lastBlock.getId(), offerId, sellerId, buyerId, units,
            rateATM, lastBlock.getTimestamp(), lastBlock.getHeight());
        exchangeTable.insert(exchange);
        return exchange;
    }


    @Override
    public DbIterator<Exchange> getAllExchanges(int from, int to) {
        return exchangeTable.getAll(from, to);
    }

    @Override
    public Stream<Exchange> getAllExchangesStream(int from, int to) {
        return exchangeIteratorToStreamConverter.apply( exchangeTable.getAll(from, to) );
    }

    @Override
    public int getCount() {
        return exchangeTable.getCount();
    }

    @Override
    public DbIterator<Exchange> getCurrencyExchanges(long currencyId, int from, int to) {
        return exchangeTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    @Override
    public Stream<Exchange> getCurrencyExchangesStream(long currencyId, int from, int to) {
        return exchangeIteratorToStreamConverter.apply(
            exchangeTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to));
    }

    @Override
    public List<Exchange> getLastExchanges(long[] currencyIds) {
        return exchangeTable.getLastExchanges(currencyIds);
    }

    @Override
    public DbIterator<Exchange> getAccountExchanges(long accountId, int from, int to) {
        return exchangeTable.getAccountExchanges(accountId, from, to);
    }

    @Override
    public Stream<Exchange> getAccountExchangesStream(long accountId, int from, int to) {
        return exchangeIteratorToStreamConverter.apply(exchangeTable.getAccountExchanges(accountId, from, to));
    }

    @Override
    public DbIterator<Exchange> getAccountCurrencyExchanges(long accountId, long currencyId, int from, int to) {
        return exchangeTable.getAccountCurrencyExchanges(accountId, currencyId, from, to);
    }

    @Override
    public Stream<Exchange> getAccountCurrencyExchangesStream(long accountId, long currencyId, int from, int to) {
        return exchangeIteratorToStreamConverter.apply(
            exchangeTable.getAccountCurrencyExchanges(accountId, currencyId, from, to));
    }

    @Override
    public DbIterator<Exchange> getExchanges(long transactionId) {
        return exchangeTable.getManyBy(new DbClause.LongClause("transaction_id", transactionId), 0, -1);
    }

    @Override
    public Stream<Exchange> getExchangesStream(long transactionId) {
        return exchangeIteratorToStreamConverter.apply(
            exchangeTable.getManyBy(new DbClause.LongClause("transaction_id", transactionId), 0, -1));
    }

    @Override
    public DbIterator<Exchange> getOfferExchanges(long offerId, int from, int to) {
        return exchangeTable.getManyBy(new DbClause.LongClause("offer_id", offerId), from, to);
    }

    @Override
    public Stream<Exchange> getOfferExchangesStream(long offerId, int from, int to) {
        return exchangeIteratorToStreamConverter.apply(
            exchangeTable.getManyBy(new DbClause.LongClause("offer_id", offerId), from, to));
    }

    @Override
    public int getExchangeCount(long currencyId) {
        return exchangeTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

}
