/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.trade.service.impl;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.converter.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.order.entity.AskOrder;
import com.apollocurrency.aplwallet.apl.core.order.entity.BidOrder;
import com.apollocurrency.aplwallet.apl.core.trade.dao.TradeTable;
import com.apollocurrency.aplwallet.apl.core.trade.entity.Trade;
import com.apollocurrency.aplwallet.apl.core.trade.model.Event;
import com.apollocurrency.aplwallet.apl.core.trade.service.TradeService;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class TradeServiceImpl implements TradeService {
    private final DatabaseManager databaseManager;
    private final Blockchain blockchain;
    private final IteratorToStreamConverter<Trade> converter;
    private final TradeTable tradeTable;
    private final Listeners<Trade, Event> listeners;

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
        this.listeners = new Listeners<>();
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
    public boolean addListener(Listener<Trade> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<Trade> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
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
        listeners.notify(trade, Event.TRADE);
        return trade;
    }
}
