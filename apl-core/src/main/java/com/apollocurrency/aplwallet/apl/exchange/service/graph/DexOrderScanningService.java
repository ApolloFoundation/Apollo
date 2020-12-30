package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderScan;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
@Slf4j
public class DexOrderScanningService {
    static final String SERVICE_NAME = "CandlesticksScanner";
    private static final int DEFAULT_ORDER_SELECT_LIMIT = 100;
    private static final String TASK_NAME = "OrderProcessor";
    private static final int ORDER_SCANNING_HEIGHT_OFFSET = 50_000;
    private static final int ORDER_SCANNING_DELAY = 5 * 60 * 1000; // 5 minutes in ms

    private final DexCandlestickDao candlestickDao;
    private final DexOrderDao dexOrderDao;
    private final TaskDispatchManager taskDispatchManager;
    private final Blockchain blockchain;
    private final int orderSelectLimit;
    private final ScanPerformer scanPerformer;


    private final Lock lock = new ReentrantLock();
    private boolean blockchainScanInProgress;

    public DexOrderScanningService(ScanPerformer performer, DexCandlestickDao candlestickDao, DexOrderDao dexOrderDao, TaskDispatchManager taskDispatchManager, Blockchain blockchain, int orderSelectLimit) {
        this.candlestickDao = candlestickDao;
        this.dexOrderDao = dexOrderDao;
        this.taskDispatchManager = taskDispatchManager;
        this.blockchain = blockchain;
        this.orderSelectLimit = orderSelectLimit;
        this.scanPerformer = performer;
    }

    @Inject
    public DexOrderScanningService(ScanPerformer performer, Blockchain blockchain, DexCandlestickDao candlestickDao, DexOrderDao dexOrderDao, TaskDispatchManager taskDispatchManager) {
        this(performer, candlestickDao, dexOrderDao, taskDispatchManager, blockchain, DEFAULT_ORDER_SELECT_LIMIT);
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

    void tryScan() {
        int height = blockchain.getHeight();
        int toHeight = height - ORDER_SCANNING_HEIGHT_OFFSET;
        try {
            startScan(toHeight);
        } catch (Exception e) {
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
        int orders;
        do {
            lock.lock();
            try {
                if (blockchainScanInProgress) {
                    return;
                }
                orders = scanPerformer.doIteration(currency, toHeight, orderSelectLimit);
            } finally {
                lock.unlock();
            }
            ThreadUtils.sleep(200);
        } while (orders == orderSelectLimit);
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
                    scanPerformer.saveOrderScan(new OrderScan(cur, dbId));
                }
            }
        } finally {
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
