/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.exchange.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeRequestTable;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeRequestService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeSell;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class ExchangeRequestServiceImpl implements ExchangeRequestService {

    private final ExchangeRequestTable exchangeRequestTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<ExchangeRequest> exchangeRequestIteratorToStreamConverter;

    @Inject
    public ExchangeRequestServiceImpl(ExchangeRequestTable exchangeRequestTable,
                                      BlockChainInfoService blockChainInfoService) {
        this.exchangeRequestTable = exchangeRequestTable;
        this.blockChainInfoService = blockChainInfoService;
        this.exchangeRequestIteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * for unit tests
     */
    public ExchangeRequestServiceImpl(ExchangeRequestTable exchangeRequestTable,
                                      BlockChainInfoService blockChainInfoService,
                                      IteratorToStreamConverter<ExchangeRequest> exchangeRequestIteratorToStreamConverter) {
        this.exchangeRequestTable = exchangeRequestTable;
        this.blockChainInfoService = blockChainInfoService;
        if (exchangeRequestIteratorToStreamConverter != null) {
            this.exchangeRequestIteratorToStreamConverter = exchangeRequestIteratorToStreamConverter;
        } else {
            this.exchangeRequestIteratorToStreamConverter = new IteratorToStreamConverter<>();
        }
    }

    @Override
    public DbIterator<ExchangeRequest> getAllExchangeRequests(int from, int to) {
        return exchangeRequestTable.getAll(from, to);
    }

    @Override
    public Stream<ExchangeRequest> getAllExchangeRequestsStream(int from, int to) {
        return exchangeRequestIteratorToStreamConverter.apply(exchangeRequestTable.getAll(from, to));
    }

    @Override
    public int getCount() {
        return exchangeRequestTable.getCount();
    }

    @Override
    public ExchangeRequest getExchangeRequest(long transactionId) {
        return exchangeRequestTable.get(ExchangeRequestTable.exchangeRequestDbKeyFactory.newKey(transactionId));
    }

    @Override
    public DbIterator<ExchangeRequest> getCurrencyExchangeRequests(long currencyId, int from, int to) {
        return exchangeRequestTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    @Override
    public Stream<ExchangeRequest> getCurrencyExchangeRequestsStream(long currencyId, int from, int to) {
        return exchangeRequestIteratorToStreamConverter.apply(
            exchangeRequestTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to));
    }

    @Override
    public DbIterator<ExchangeRequest> getAccountExchangeRequests(long accountId, int from, int to) {
        return exchangeRequestTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    @Override
    public Stream<ExchangeRequest> getAccountExchangeRequestsStream(long accountId, int from, int to) {
        return exchangeRequestIteratorToStreamConverter.apply(
            exchangeRequestTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to));
    }

    @Override
    public DbIterator<ExchangeRequest> getAccountCurrencyExchangeRequests(long accountId, long currencyId, int from, int to) {
        return exchangeRequestTable.getManyBy(
            new DbClause.LongClause("account_id", accountId)
                .and(new DbClause.LongClause("currency_id", currencyId)), from, to);
    }

    @Override
    public Stream<ExchangeRequest> getAccountCurrencyExchangeRequestsStream(long accountId, long currencyId, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId)
            .and(new DbClause.LongClause("currency_id", currencyId));
        return exchangeRequestIteratorToStreamConverter.apply(exchangeRequestTable.getManyBy(dbClause, from, to));
    }

    @Override
    public void addExchangeRequest(Transaction transaction, MonetarySystemExchangeBuyAttachment attachment) {
        Block lastBlock = blockChainInfoService.getLastBlock();
        ExchangeRequest exchangeRequest = new ExchangeRequest(
            transaction, attachment, lastBlock.getTimestamp(), lastBlock.getHeight());
        exchangeRequestTable.insert(exchangeRequest);
    }

    @Override
    public void addExchangeRequest(Transaction transaction, MonetarySystemExchangeSell attachment) {
        Block lastBlock = blockChainInfoService.getLastBlock();
        ExchangeRequest exchangeRequest = new ExchangeRequest(
            transaction, attachment, lastBlock.getTimestamp(), lastBlock.getHeight());
        exchangeRequestTable.insert(exchangeRequest);
    }


}
