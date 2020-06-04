/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.dao.state.TradeTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.Trade;
import com.apollocurrency.aplwallet.apl.core.service.state.TradeService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class TradeServiceImpl implements TradeService {
    private final DatabaseManager databaseManager;
    private final Blockchain blockchain;
    private final IteratorToStreamConverter<Trade> converter;
    private final TradeTable tradeTable;

    /**
     * Constructor for unit tests.
     *
     * @param databaseManager
     * @param blockchain
     * @param tradeTable
     * @param converter
     */
    public TradeServiceImpl(
        final DatabaseManager databaseManager,
        final Blockchain blockchain,
        final TradeTable tradeTable,
        final IteratorToStreamConverter<Trade> converter
    ) {
        this.databaseManager = databaseManager;
        this.blockchain = blockchain;
        this.tradeTable = tradeTable;
        this.converter = converter;
    }

    @Inject
    public TradeServiceImpl(
        final DatabaseManager databaseManager,
        final Blockchain blockchain,
        final TradeTable tradeTable
    ) {
        this.databaseManager = databaseManager;
        this.blockchain = blockchain;
        this.tradeTable = tradeTable;
        this.converter = new IteratorToStreamConverter<>();
    }

    @Override
    public Stream<Trade> getAllTrades(int from, int to) {
        return converter.convert(tradeTable.getAll(from, to));
    }

    @Override
    public int getCount() {
        return tradeTable.getCount();
    }

    @Override
    public Trade getTrade(long askOrderId, long bidOrderId) {
        return tradeTable.getTrade(askOrderId, bidOrderId);
    }

    @Override
    public Stream<Trade> getAssetTrades(long assetId, int from, int to) {
        return converter.convert(
            tradeTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to)
        );
    }

    @Override
    public List<Trade> getLastTrades(long[] assetIds) {
        final TransactionalDataSource dataSource = databaseManager.getDataSource();
        return tradeTable.getLastTrades(dataSource, assetIds);
    }

    @Override
    public Stream<Trade> getAccountTrades(long accountId, int from, int to) {
        final TransactionalDataSource dataSource = databaseManager.getDataSource();
        return converter.convert(
            tradeTable.getAccountTrades(dataSource, accountId, from, to)
        );
    }

    @Override
    public Stream<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to) {
        final TransactionalDataSource dataSource = databaseManager.getDataSource();
        return converter.convert(
            tradeTable.getAccountAssetTrades(dataSource, accountId, assetId, from, to)
        );
    }

    @Override
    public Stream<Trade> getAskOrderTrades(long askOrderId, int from, int to) {
        return converter.convert(
            tradeTable.getManyBy(
                new DbClause.LongClause("ask_order_id", askOrderId), from, to
            )
        );
    }

    @Override
    public Stream<Trade> getBidOrderTrades(long bidOrderId, int from, int to) {
        return converter.convert(
            tradeTable.getManyBy(
                new DbClause.LongClause("bid_order_id", bidOrderId), from, to
            )
        );
    }

    @Override
    public int getTradeCount(long assetId) {
        return tradeTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    @Override
    public Trade addTrade(long assetId, AskOrder askOrder, BidOrder bidOrder) {
        final Block block = blockchain.getLastBlock();
        final DbKey dbKey = tradeTable.getDbKey(askOrder.getId(), bidOrder.getId());
        log.trace(">> addTrade() newDbKey={}, assetId={}, askOrder={}, bidOrder={}, height={}",
            dbKey, assetId, askOrder, bidOrder, block.getHeight());
        Trade trade = new Trade(
            assetId,
            askOrder,
            bidOrder,
            dbKey,
            block.getId(),
            block.getHeight(),
            block.getTimestamp()
        );
        tradeTable.insert(trade);
        log.trace("<< addTrade() assetId={}, askOrder={}, bidOrder={}, trade={}", assetId, askOrder, bidOrder, trade);
        return trade;
    }
}
