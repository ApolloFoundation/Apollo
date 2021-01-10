package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.HeightDbIdRequest;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderScan;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.OrderScanDao;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickUtil.convertOrders;

// was created to perform each scanning iteration in a separate transaction
@Singleton
public class ScanPerformer {
    private OrderScanDao orderScanDao;
    private DexOrderDao dexOrderDao;
    private DexCandlestickDao candlestickDao;

    @Inject
    public ScanPerformer(OrderScanDao orderScanDao, DexOrderDao dexOrderDao, DexCandlestickDao candlestickDao) {
        this.orderScanDao = orderScanDao;
        this.dexOrderDao = dexOrderDao;
        this.candlestickDao = candlestickDao;
    }

    @Transactional
    public int doIteration(DexCurrency currency, int toHeight, int limit) {
        long fromDbId = getFromDbId(currency);
        List<DexOrder> orders = dexOrderDao.getClosedOrdersFromDbId(HeightDbIdRequest.builder()
            .coin(currency)
            .toHeight(toHeight)
            .fromDbId(fromDbId)
            .limit(limit)
            .build());
        if (!orders.isEmpty()) {
            List<DexCandlestick> candlesticks = mapOrdersToCandlesticks(orders, currency);
            saveCandlesticks(candlesticks);
            DexOrder lastOrder = orders.get(orders.size() - 1);
            fromDbId = lastOrder.getDbId();
            saveOrderScan(new OrderScan(currency, fromDbId));
        }
        return orders.size();
    }

    private long getFromDbId(DexCurrency currency) {
        OrderScan orderScan = orderScanDao.get(currency);
        long dbId = 0;
        if (orderScan != null) {
            dbId = orderScan.getLastDbId();
        }
        return dbId;
    }

    void saveOrderScan(OrderScan orderScan) {
        OrderScan existing = orderScanDao.get(orderScan.getCoin());
        if (existing == null) {
            orderScanDao.add(orderScan);
        } else {
            orderScanDao.update(orderScan);
        }
    }

    private void saveCandlesticks(List<DexCandlestick> candlesticks) {
        candlesticks.forEach(c -> {
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
        convertOrders(orders, candlesticks, TimeFrame.QUARTER, (time) -> candlestickDao.getByTimestamp(time, currency));
        return new ArrayList<>(candlesticks.values());
    }

}
