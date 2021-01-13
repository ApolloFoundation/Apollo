package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.OrderScanDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DBSortOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequestForTrading;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderSortBy;
import com.apollocurrency.aplwallet.apl.exchange.model.HeightDbIdRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderDbIdPaginationDbRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderScan;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.apl;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.dec;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.eCandlestick;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.eOrder;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.pCandlestick;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.pOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class DexOrderScanningServiceUnitTest {
    @Mock
    Blockchain blockchain;
    @Mock
    TaskDispatchManager dispatchManager;

    private InMemoryOrderDao orderDao;
    private ScanPerformer scanPerformer;
    private OrderScanDao orderScanDao;
    private DexCandlestickDao candlestickDao;
    private DexOrderScanningService service;


    @BeforeEach
    void setUp() {
        GenesisImporter.EPOCH_BEGINNING = 0;
        orderDao = new InMemoryOrderDao();
        candlestickDao = new InMemoryDexCandlestickDao();
        orderScanDao = new InMemoryOrderScanDao();
        scanPerformer = new ScanPerformer(orderScanDao, orderDao, candlestickDao);
        service = new DexOrderScanningService(scanPerformer, candlestickDao, orderDao, dispatchManager, blockchain, 2);
    }

    @Test
    void testTryScan() {
        orderScanDao.add(new OrderScan(DexCurrency.ETH, 2));
        doReturn(50100).when(blockchain).getHeight();

        List<DexOrder> orders = List.of(
            eOrder(1L, 3801, dec("1.2"), apl(100_000), 80),
            eOrder(2L, 3605, dec("1.25"), apl(150_000), 85),
            eOrder(3L, 4621, dec("1.3"), apl(200_000), 90),
            eOrder(4L, 3602, dec("1.35"), apl(250_000), 91),
            eOrder(5L, 4620, dec("1.5"), apl(300_000), 91),
            eOrder(6L, 4602, dec("1.35"), apl(250_000), 96),
            eOrder(7L, 5600, dec("1.5"), apl(20_000), 100),

            pOrder(8L, 2702, dec("2.2"), apl(10_000), 2),
            pOrder(9L, 2802, dec("2.5"), apl(50_000), 10),
            pOrder(10L, 3599, dec("0.3"), apl(400_000), 42),
            pOrder(11L, 3606, dec("2.3"), apl(350_000), 42),
            pOrder(12L, 3720, dec("2.5"), apl(200_000), 42),
            pOrder(13L, 3900, dec("2.4"), apl(150_000), 96),
            pOrder(14L, 4400, dec("1.1"), apl(200_000), 100) // last candlestick was not finished
        );
        orderDao.add(orders);

        candlestickDao.add(
            eCandlestick("1.2", "1.25", "1.25", "1.2", "250000", "307500.00", 3600, 3605, 3801)
        );

        service.tryScan();

        OrderScan paxOrderScan = orderScanDao.get(DexCurrency.PAX);
        assertEquals(new OrderScan(DexCurrency.PAX, 13), paxOrderScan);
        OrderScan ethOrderScan = orderScanDao.get(DexCurrency.ETH);
        assertEquals(new OrderScan(DexCurrency.ETH, 6), ethOrderScan);
        List<DexCandlestick> ethCandlesticks = candlestickDao.getForTimespan(0, Integer.MAX_VALUE, DexCurrency.ETH);
        assertEquals(List.of(
            eCandlestick("1.2", "1.35", "1.35", "1.2", "500000", "645000.00", 3600, 3602, 3801),
            eCandlestick("1.3", "1.5", "1.35", "1.3", "750000", "1047500.00", 4500, 4602, 4620)
        ), ethCandlesticks);
        List<DexCandlestick> paxCandlesticks = candlestickDao.getForTimespan(0, Integer.MAX_VALUE, DexCurrency.PAX);
        assertEquals(List.of(
            pCandlestick("0.3", "2.5", "2.2", "0.3", "460000", "267000.0", 2700, 2701, 3599),
            pCandlestick("2.3", "2.5", "2.3", "2.4", "700000", "1665000.0", 3600, 3605, 3900)
        ), paxCandlesticks);


    }

    private HeightDbIdRequest request(long fromDbId, int toHeight, int limit) {
        return HeightDbIdRequest.builder()
            .fromDbId(fromDbId)
            .toHeight(toHeight)
            .limit(limit)
            .coin(DexCurrency.ETH)
            .build();
    }

    // In memory dao impls to simulate database
    // Act as a real dao, but without db transactions
    private static class InMemoryDexCandlestickDao implements DexCandlestickDao {
        private Map<CurrencyAndTime, DexCandlestick> candlesticks = new ConcurrentHashMap<>();

        @Override
        public List<DexCandlestick> getForTimespan(int fromTimestamp, int toTimestamp, DexCurrency pairedCoin) {
            return candlesticks.values().stream().filter(c -> c.getTimestamp() >= fromTimestamp && c.getTimestamp() <= toTimestamp && c.getCoin() == pairedCoin).sorted(Comparator.comparing(DexCandlestick::getTimestamp)).collect(Collectors.toList());
        }

        @Override
        public DexCandlestick getByTimestamp(int timestamp, DexCurrency pairedCoin) {
            return candlesticks.get(new CurrencyAndTime(pairedCoin, timestamp));
        }

        @Override
        public DexCandlestick getLast(DexCurrency pairedCoin) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DexCandlestick getLast() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DexCandlestick getLast(DexCurrency pairedCoin, int beforeTimestamp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int removeAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int removeAfterTimestamp(int timestamp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(DexCandlestick candlestick) {
            if (getByTimestamp(candlestick.getTimestamp(), candlestick.getCoin()) != null) {
                throw new IllegalArgumentException("Unable to add, already exists");
            }
            candlesticks.put(new CurrencyAndTime(candlestick.getCoin(), candlestick.getTimestamp()), candlestick);
        }

        @Override
        public void update(DexCandlestick candlestick) {
            if (getByTimestamp(candlestick.getTimestamp(), candlestick.getCoin()) == null) {
                throw new IllegalArgumentException("Unable to update, does not exist");
            }
            candlesticks.replace(new CurrencyAndTime(candlestick.getCoin(), candlestick.getTimestamp()), candlestick);
        }

        @Data
        @AllArgsConstructor
        private static class CurrencyAndTime {
            private DexCurrency currency;
            private int timestamp;

        }
    }

    private static class InMemoryOrderScanDao implements OrderScanDao {
        private Map<DexCurrency, OrderScan> scans = new ConcurrentHashMap<>();

        @Override
        public void add(OrderScan orderScan) {
            if (get(orderScan.getCoin()) != null) {
                throw new IllegalArgumentException("Unable to add, alredy exists");
            }
            scans.put(orderScan.getCoin(), orderScan);
        }

        @Override
        public void update(OrderScan orderScan) {
            if (get(orderScan.getCoin()) == null) {
                throw new IllegalArgumentException("Unable to update, does not exist");
            }
            scans.put(orderScan.getCoin(), orderScan);
        }

        @Override
        public OrderScan get(DexCurrency coin) {
            return scans.get(coin);
        }
    }

    private static class InMemoryOrderDao implements DexOrderDao {
        List<DexOrder> orders = Collections.synchronizedList(new ArrayList<>());

        @Override
        public List<DexOrder> getOrders(DexOrderDBRequest dexOrderDBRequest, DexOrderSortBy sortBy, DBSortOrder sort) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DexOrder> getOffersForMatchingPure(DexOrderDBMatchingRequest dexOrderDBMatchingRequest, String orderBy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DexOrder> getClosedOrdersFromDbId(HeightDbIdRequest heightDbIdRequest) {
            return orders.stream()
                .filter(o ->
                    o.getHeight() < heightDbIdRequest.getToHeight()
                        && o.getPairCurrency() == heightDbIdRequest.getCoin()
                        && o.getDbId() > heightDbIdRequest.getFromDbId())
                .sorted(Comparator.comparing(DexOrder::getDbId))
                .limit(heightDbIdRequest.getLimit())
                .collect(Collectors.toList());
        }

        @Override
        public DexOrder getLastClosedOrderBeforeHeight(DexCurrency coin, int toHeight) {
            return orders.stream().filter(o -> o.getHeight() < toHeight && o.getPairCurrency() == coin).max(Comparator.comparing(DexOrder::getDbId)).get();
        }

        @Override
        public DexOrder getLastClosedOrderBeforeTimestamp(DexCurrency coin, int timestamp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DexOrder> getOrdersForTrading(DexOrderDBRequestForTrading dexOrderDBRequestForTrading) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DexOrder> getOrdersFromDbIdBetweenTimestamps(OrderDbIdPaginationDbRequest request) {
            throw new UnsupportedOperationException();
        }

        public void add(DexOrder... orders) {
            add(Arrays.asList(orders));
        }

        public void add(Collection<DexOrder> orders) {
            this.orders.addAll(orders);
        }
    }

}