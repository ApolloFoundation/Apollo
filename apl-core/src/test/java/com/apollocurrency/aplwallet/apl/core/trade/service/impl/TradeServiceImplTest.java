package com.apollocurrency.aplwallet.apl.core.trade.service.impl;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.converter.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.order.entity.AskOrder;
import com.apollocurrency.aplwallet.apl.core.order.entity.BidOrder;
import com.apollocurrency.aplwallet.apl.core.trade.dao.TradeTable;
import com.apollocurrency.aplwallet.apl.core.trade.entity.Trade;
import com.apollocurrency.aplwallet.apl.core.trade.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author silaev-firstbridge on 4/13/2020
 */
@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {
    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private Blockchain blockchain;

    @SuppressWarnings("unchecked")
    private IteratorToStreamConverter<Trade> converter = mock(IteratorToStreamConverter.class);

    @Mock
    private TradeTable tradeTable;

    @Mock
    private TradeService tradeService;

    @BeforeEach
    void setUp() {
        this.tradeService = new TradeServiceImpl(
            databaseManager,
            blockchain,
            tradeTable,
            converter
        );
    }

    @Test
    void shouldGetAllTrades() {
        //GIVEN
        final int from = 0;
        final int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<Trade> dbIterator = mock(DbIterator.class);
        when(tradeTable.getAll(from, to)).thenReturn(dbIterator);
        final Trade trade = mock(Trade.class);
        final Stream<Trade> tradesExpected = Stream.of(trade);
        when(converter.convert(dbIterator)).thenReturn(tradesExpected);

        //WHEN
        final Stream<Trade> tradesActual = tradeService.getAllTrades(from, to);

        //THEN
        assertEquals(tradesExpected, tradesActual);
    }

    @Test
    void shouldGetCount() {
        //GIVEN
        final int count = 100;
        when(tradeTable.getCount()).thenReturn(100);

        //WHEN
        final int countActual = tradeService.getCount();

        //THEN
        assertEquals(count, countActual);
    }

    @Test
    void shouldGetTrade() {
        //GIVEN
        final long askOrderId = 1L;
        final long bidOrderId = 2L;
        final Trade trade = mock(Trade.class);
        when(tradeTable.getTrade(askOrderId, bidOrderId)).thenReturn(trade);

        //WHEN
        final Trade tradeActual = tradeService.getTrade(askOrderId, bidOrderId);

        //THEN
        assertEquals(trade, tradeActual);
    }

    @Test
    void shouldGetAssetTrades() {
        //GIVEN
        final long assetId = 1L;
        final int from = 0;
        final int to = 100;
        final Trade trade = mock(Trade.class);
        @SuppressWarnings("unchecked") final DbIterator<Trade> dbIterator = mock(DbIterator.class);
        when(tradeTable.getManyBy(any(DbClause.LongClause.class), eq(from), eq(to))).thenReturn(dbIterator);
        final Stream<Trade> tradesExpected = Stream.of(trade);
        when(converter.convert(dbIterator)).thenReturn(tradesExpected);

        //WHEN
        final Stream<Trade> tradesActual = tradeService.getAssetTrades(assetId, from, to);

        //THEN
        assertEquals(tradesExpected, tradesActual);
    }

    @Test
    void shouldGetLastTrades() {
        //GIVEN
        final long[] assetIds = new long[]{1};
        final Trade trade = mock(Trade.class);
        final TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        when(databaseManager.getDataSource()).thenReturn(dataSource);
        final List<Trade> tradesExpected = List.of(trade);
        when(tradeTable.getLastTrades(dataSource, assetIds)).thenReturn(tradesExpected);

        //WHEN
        final List<Trade> tradesActual = tradeService.getLastTrades(assetIds);

        //THEN
        assertEquals(tradesExpected, tradesActual);
    }

    @Test
    void shouldGetAccountTrades() {
        //GIVEN
        final long accountId = 100L;
        final int from = 0;
        final int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<Trade> dbIterator = mock(DbIterator.class);
        final Trade trade = mock(Trade.class);
        final TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        when(databaseManager.getDataSource()).thenReturn(dataSource);
        when(tradeTable.getAccountTrades(dataSource, accountId, from, to)).thenReturn(dbIterator);
        final Stream<Trade> tradesExpected = Stream.of(trade);
        when(converter.convert(dbIterator)).thenReturn(tradesExpected);

        //WHEN
        final Stream<Trade> tradesActual = tradeService.getAccountTrades(accountId, from, to);

        //THEN
        assertEquals(tradesExpected, tradesActual);
    }

    @Test
    void shouldGetAccountAssetTrades() {
        //GIVEN
        final long accountId = 200L;
        final long assetId = 6;
        final int from = 0;
        final int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<Trade> dbIterator = mock(DbIterator.class);
        final Trade trade = mock(Trade.class);
        final TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        when(databaseManager.getDataSource()).thenReturn(dataSource);
        when(tradeTable.getAccountAssetTrades(dataSource, accountId, assetId, from, to))
            .thenReturn(dbIterator);
        final Stream<Trade> tradesExpected = Stream.of(trade);
        when(converter.convert(dbIterator)).thenReturn(tradesExpected);

        //WHEN
        final Stream<Trade> tradesActual = tradeService.getAccountAssetTrades(accountId, assetId, from, to);

        //THEN
        assertEquals(tradesExpected, tradesActual);
    }

    @Test
    void shouldGetAskOrderTrades() {
        //GIVEN
        final long askOrderId = 500L;
        final int from = 0;
        final int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<Trade> dbIterator = mock(DbIterator.class);
        final Trade trade = mock(Trade.class);
        when(tradeTable.getManyBy(any(DbClause.LongClause.class), eq(from), eq(to)))
            .thenReturn(dbIterator);
        final Stream<Trade> tradesExpected = Stream.of(trade);
        when(converter.convert(dbIterator)).thenReturn(tradesExpected);

        //WHEN
        final Stream<Trade> tradesActual =
            tradeService.getAskOrderTrades(askOrderId, from, to);

        //THEN
        assertEquals(tradesExpected, tradesActual);
    }

    @Test
    void shouldGetBidOrderTrades() {
        //GIVEN
        final long bidOrderId = 500L;
        final int from = 0;
        final int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<Trade> dbIterator = mock(DbIterator.class);
        final Trade trade = mock(Trade.class);
        when(tradeTable.getManyBy(any(DbClause.LongClause.class), eq(from), eq(to)))
            .thenReturn(dbIterator);
        final Stream<Trade> tradesExpected = Stream.of(trade);
        when(converter.convert(dbIterator)).thenReturn(tradesExpected);

        //WHEN
        final Stream<Trade> tradesActual =
            tradeService.getBidOrderTrades(bidOrderId, from, to);

        //THEN
        assertEquals(tradesExpected, tradesActual);
    }

    @Test
    void shouldGetTradeCount() {
        //GIVEN
        final long assetId = 10L;
        final Trade trade = mock(Trade.class);
        final int tradeCountExpected = 400;
        when(tradeTable.getCount(any(DbClause.LongClause.class)))
            .thenReturn(tradeCountExpected);

        //WHEN
        final int tradeCountActual = tradeService.getTradeCount(assetId);

        //THEN
        assertEquals(tradeCountExpected, tradeCountActual);
    }

    @Test
    void addTrade() {
        //GIVEN
        final long assetId = 1L;
        final AskOrder askOrder = mock(AskOrder.class);
        final BidOrder bidOrder = mock(BidOrder.class);
        final Block block = mock(Block.class);
        when(blockchain.getLastBlock()).thenReturn(block);
        final long askOrderId = 8L;
        final long bidOrderId = 9L;
        when(askOrder.getId()).thenReturn(askOrderId);
        when(bidOrder.getId()).thenReturn(bidOrderId);
        final DbKey dbKey = mock(DbKey.class);
        when(tradeTable.getDbKey(askOrderId, bidOrderId)).thenReturn(dbKey);
        final long blockId = 5L;
        when(block.getId()).thenReturn(blockId);
        final int height = 1040;
        when(block.getHeight()).thenReturn(height);
        final int timestamp = 999999;
        when(block.getTimestamp()).thenReturn(timestamp);
        final Trade trade = new Trade(
            assetId,
            askOrder,
            bidOrder,
            dbKey,
            block.getId(),
            block.getHeight(),
            block.getTimestamp()
        );

        //WHEN
        final Trade tradeActual = tradeService.addTrade(assetId, askOrder, bidOrder);

        //THEN
        verify(tradeTable).insert(trade);
    }
}