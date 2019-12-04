package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import static com.apollocurrency.aplwallet.apl.exchange.service.graph.DexTradingDataService.BASE_TIME_INTERVAL;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.OrderScanDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.HeightDbIdRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderScan;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
@Slf4j
public class DexTradingGraphScanningService {
    private static final int ORDER_SELECT_LIMIT = 100;
    private static final String SERVICE_NAME = "CandlesticksScanner";
    private static final String TASK_NAME = "OrderProcessor";
    private static final int ORDER_SCANNING_HEIGHT_OFFSET = 50_000;
    private static final int ORDER_SCANNING_DELAY = 5 * 60 * 1000; // 5 minutes in ms

    private final DexCandlestickDao candlestickDao;
    private final DexOrderDao dexOrderDao;
    private final TaskDispatchManager taskDispatchManager;
    private final Blockchain blockchain;
    private final OrderScanDao orderScanDao;


    private final Lock lock = new ReentrantLock();
    private boolean blockchainScanInProgress;

    @Inject
    public DexTradingGraphScanningService(OrderScanDao orderScanDao, Blockchain blockchain, DexCandlestickDao candlestickDao, DexOrderDao dexOrderDao, TaskDispatchManager taskDispatchManager) {
        this.blockchain = blockchain;
        this.candlestickDao = candlestickDao;
        this.dexOrderDao = dexOrderDao;
        this.taskDispatchManager = taskDispatchManager;
        this.orderScanDao = orderScanDao;
    }

    @PostConstruct
    public void init() {
        taskDispatchManager.newScheduledDispatcher(SERVICE_NAME)
                .schedule(Task.builder()
                        .name(TASK_NAME)
                        .task(this::tryScan)
                        .delay(ORDER_SCANNING_DELAY)
                        .build());
    }

    private void tryScan() {
        int height = blockchain.getHeight();
        int toHeight = height - ORDER_SCANNING_HEIGHT_OFFSET;
        try {
            startScan(toHeight);
        }
        catch (Exception e) {
            log.error("Error during order scanning, end height - " + toHeight, e);
        }
    }

    private void startScan(int toHeight) {
        for (DexCurrency currency : DexCurrency.values()) {
            if (currency != DexCurrency.APL) {
                scanForCurrency(toHeight, currency);
            }
        }
    }

    private void scanForCurrency(int toHeight, DexCurrency currency) {
        List<DexOrder> orders;
        do {
            lock.lock();
            try {
                if (blockchainScanInProgress) {
                    return;
                }
                long fromDbId = getFromDbId(currency);
                orders = dexOrderDao.getClosedOrdersFromDbId(HeightDbIdRequest.builder()
                        .coin((byte) currency.ordinal())
                        .toHeight(toHeight)
                        .fromDbId(fromDbId)
                        .limit(ORDER_SELECT_LIMIT)
                        .build());
                List<DexCandlestick> candlesticks = mapOrdersToCandlesticks(orders, currency);

                saveCandlesticks(candlesticks);
                if (!orders.isEmpty()) {
                    DexOrder lastOrder = last(orders);
                    fromDbId = lastOrder.getDbId();
                    saveOrderScan(new OrderScan(currency, last(candlesticks).getTimestamp(), fromDbId));
                }
            }
            finally {
                lock.unlock();
            }
        } while (orders.size() == ORDER_SELECT_LIMIT);
    }

    private long getFromDbId(DexCurrency currency) {
        OrderScan orderScan = orderScanDao.get(currency);
        long dbId = 0;
        if (orderScan != null) {
            dbId = orderScan.getLastDbId();
        }
        return dbId;
    }

    private <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }

    private void saveOrderScan(OrderScan orderScan) {
        OrderScan existing = orderScanDao.get(orderScan.getCoin());
        if (existing == null) {
            orderScanDao.add(orderScan);
        } else {
            orderScanDao.update(orderScan);
        }
    }

    private void saveCandlesticks(List<DexCandlestick> candlesticks) {
        candlesticks.forEach(c-> {
            DexCandlestick existingCandlestick = candlestickDao.getByTimestamp(c.getTimestamp(), c.getCoin());
            if (existingCandlestick != null) {
                candlestickDao.update(c);
            } else {
                candlestickDao.add(c);
            }
        });
    }

    private List<DexCandlestick> mapOrdersToCandlesticks(List<DexOrder> orders, DexCurrency currency) {
        Map<Integer, DexCandlestick> candlesticks = new HashMap<>();
        DexCandlestick lastCandlestick = candlestickDao.getLast(currency);
        candlesticks.put(lastCandlestick.getTimestamp(), lastCandlestick);
        convertOrders(orders, candlesticks);
        return new ArrayList<>(candlesticks.values());
    }

    private void convertOrders(List<DexOrder> orders, Map<Integer, DexCandlestick> candlesticks) {
        for (DexOrder order : orders) {
            int unixEpochSeconds = (int) (Convert2.fromEpochTime(order.getFinishTime()) / 1000);
            int openTime = unixEpochSeconds % BASE_TIME_INTERVAL;
            DexCandlestick dexCandlestick = candlesticks.get(openTime);
            candlesticks.put(openTime, toCandlestick(order, dexCandlestick, openTime));
        }
    }

    private DexCandlestick toCandlestick(DexOrder order, DexCandlestick thisCandlestick, int candlestickTime) {
        if (thisCandlestick != null) {
            thisCandlestick.setFromVolume(thisCandlestick.getFromVolume().add(EthUtil.atmToEth(order.getOrderAmount())));
            BigDecimal pairedCurrencyVolume = order.getPairRate().multiply(EthUtil.atmToEth(order.getOrderAmount()));
            thisCandlestick.setToVolume(thisCandlestick.getToVolume().add(pairedCurrencyVolume));
            thisCandlestick.setClose(order.getPairRate());
            if (thisCandlestick.getMax().compareTo(order.getPairRate()) < 0) {
                thisCandlestick.setMax(order.getPairRate());
            }
            if (thisCandlestick.getMin().compareTo(order.getPairRate()) > 0) {
                thisCandlestick.setMin(order.getPairRate());
            }
            return thisCandlestick;
        } else {
            return new DexCandlestick(order.getPairCurrency(), order.getPairRate(), order.getPairRate(), order.getPairRate(),
                    order.getPairRate(), EthUtil.atmToEth(order.getOrderAmount()), order.getPairRate().multiply(EthUtil.atmToEth(order.getOrderAmount())), candlestickTime);
        }
    }


    public void onBlockchainScanStarted(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        lock.lock();
        try {
            blockchainScanInProgress = true;
            int timestamp = block.getTimestamp();
            int blockUnixTimestamp = (int) (Convert2.fromEpochTime(timestamp) / 1000);
            candlestickDao.removeAfterTimestamp(blockUnixTimestamp);
            for (DexCurrency cur : DexCurrency.values()) {
                if (cur != DexCurrency.APL) {
                    DexOrder order = dexOrderDao.getLastClosedOrderBeforeHeight(cur, block.getHeight() + 1);
                    long dbId = 0;
                    if (order != null) {
                        dbId = order.getDbId();
                    }
                    saveOrderScan(new OrderScan(cur, 0, dbId));
                }
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void onBlockchainScanFinished(@Observes @BlockEvent(BlockEventType.RESCAN_END) Block block) {
        lock.lock();
        try {
            blockchainScanInProgress = false;
        } finally {
            lock.unlock();
        }
    }

}
