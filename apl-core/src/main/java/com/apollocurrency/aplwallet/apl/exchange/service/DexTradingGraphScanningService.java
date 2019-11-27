package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import lombok.Data;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

@Singleton
public class DexTradingGraphScanningService {
    private DexCandlestickDao candlestickDao;
    private DexOrderDao dexOrderDao;
    private volatile int lastScanHeight;
    private volatile ScanConfig config;

    @Data
    private static class ScanConfig {
        private final int fromHeight;
        private final int fromBlockTimestamp;
    }

    public void scheduleScan(int height, int blockTimestamp) {
        config = new ScanConfig(height, blockTimestamp);
    }

    public void terminateScan() {

    }

    private void startScan() {
        dexOrderDao.getOrders(DexOrderDBRequest.builder().status(OrderStatus.CLOSED).build());
    }


    public void onBlockchainScanStarted(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        int timestamp = block.getTimestamp();
        int blockUnixTimestamp = (int) (Convert2.fromEpochTime(timestamp) / 1000);
        candlestickDao.removeAfterTimestamp(blockUnixTimestamp);
        scheduleScan(block.getHeight(), block.getTimestamp());
    }

    public void onBlockchainScanFinished(@Observes @BlockEvent(BlockEventType.RESCAN_END) Block block) {
        startScan();
    }
}
