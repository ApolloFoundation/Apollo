package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.HeightDbIdRequest;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.Data;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import static com.apollocurrency.aplwallet.apl.exchange.service.graph.DexTradingDataService.BASE_TIME_INTERVAL;

@Singleton
public class DexTradingGraphScanningService {
    private static final int ORDER_SELECT_LIMIT = 100;
    private static final String SERVICE_NAME = "CandlesticksScanner";
    private static final String TASK_NAME = "OrderProcessor";
    private static final int ORDER_SCANNING_HEIGHT_OFFSET = 50_000;

    private DexCandlestickDao candlestickDao;
    private DexOrderDao dexOrderDao;
    private TaskDispatchManager taskDispatchManager;
    private Blockchain blockchain;

    private AtomicInteger lastScanHeight = new AtomicInteger();
    private volatile boolean isScanning;
    private CompletableFuture<Void> scanningProcess;

    @Inject
    public DexTradingGraphScanningService(Blockchain blockchain, DexCandlestickDao candlestickDao, DexOrderDao dexOrderDao, TaskDispatchManager taskDispatchManager) {
        this.blockchain = blockchain;
        this.candlestickDao = candlestickDao;
        this.dexOrderDao = dexOrderDao;
        this.taskDispatchManager = taskDispatchManager;
    }

    @PostConstruct
    public void init() {
        taskDispatchManager.newScheduledDispatcher(SERVICE_NAME)
                .schedule(Task.builder()
                        .name(TASK_NAME)
                        .task(this::tryScan)
                        .build());
    }

    @Data
    private static class ScanConfig {
        private final int fromHeight;
        private final int fromBlockTimestamp;
    }

    public void terminateScan() {

    }

    private void tryScan() {
        int height = blockchain.getHeight();
        int scanHeight = height - ORDER_SCANNING_HEIGHT_OFFSET;
        if (scanHeight) {

        }
    }

    private void initLastScanHeight() {
        if (lastScanHeight == 0) {
            candlestickDao.getLast().
        }
    }

    private void startScan(int fromHeight, int toHeight) {
        for (DexCurrency currency : DexCurrency.values()) {
            if (currency != DexCurrency.APL) {
                scanForCurrency(fromHeight, toHeight, currency);
            }
        }
    }

    private void scanForCurrency(int fromHeight, int toHeight, DexCurrency currency) {
        long fromDbId = 0;
        List<DexOrder> orders;
        do {
            orders = dexOrderDao.getOrdersFromHeight(HeightDbIdRequest.builder()
                    .coin((byte) currency.ordinal())
                    .fromHeight(fromHeight)
                    .toHeight(toHeight)
                    .fromDbId(fromDbId)
                    .limit(ORDER_SELECT_LIMIT)
                    .build());
            List<DexCandlestick> candlesticks = mapOrdersToCandlesticks(orders, currency);
            saveCandlesticks(candlesticks);
            if (!orders.isEmpty()) {
                fromDbId = orders.get(orders.size() - 1).getDbId();
            }
        } while (orders.size() == ORDER_SELECT_LIMIT);
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
        int timestamp = block.getTimestamp();
        int blockUnixTimestamp = (int) (Convert2.fromEpochTime(timestamp) / 1000);
        candlestickDao.removeAfterTimestamp(blockUnixTimestamp);
        lastScanHeight.getAndUpdate(operand -> Math.min(operand, block.getHeight()));
    }
}
