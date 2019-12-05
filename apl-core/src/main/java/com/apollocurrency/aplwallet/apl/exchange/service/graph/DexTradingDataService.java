package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.api.trading.ConversionType;
import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderDbIdPaginationDbRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class DexTradingDataService {
    private boolean enableTradingViewGraphDataFeeder;
    private boolean enableTradingDataCache;
    private DexCandlestickDao candlestickDao;
    private DexOrderDao orderDao;
    static final int BASE_TIME_INTERVAL = 15 * 60 * 60; // 15 minutes in seconds

    public enum TimeFrame {
        QUARTER(1), HOUR(4), FOUR_HOURS(16), DAY(96);
        private final int muliplier; // BASE TIME INTERVAL multiplier
        TimeFrame(int multiplier) {
            this.muliplier = multiplier;
        }
    }


    @Inject
    public DexTradingDataService(@Property("apl.dex.graph.enableDataFeeder") boolean enableTradingViewGraphDataFeeder,
                                 @Property("apl.dex.graph.enableDataCache") boolean enableTradingDataCache,
                                 DexCandlestickDao candlestickDao,
                                 DexOrderDao orderDao) {
        this.enableTradingViewGraphDataFeeder = enableTradingViewGraphDataFeeder;
        this.enableTradingDataCache = enableTradingDataCache;
        this.candlestickDao = candlestickDao;
        this.orderDao = orderDao;
    }

    public List<SimpleTradingEntry> getForTimeFrameFromCandlesticks(int toTimestamp, int limit, DexCurrency currency, TimeFrame timeFrame) {
        int startTime = toTimestamp - limit * timeFrame.muliplier * BASE_TIME_INTERVAL;
        List<DexCandlestick> candlesticks = candlestickDao.getFromToTimestamp(startTime, toTimestamp, currency);
        List<SimpleTradingEntry> fullData = new ArrayList<>(limit * timeFrame.muliplier);
        int prevTime = startTime;
        for (DexCandlestick candlestick : candlesticks) {
            pushUntil(prevTime, candlestick.getTimestamp(), fullData);
            fullData.add(fromCandleStick(candlestick));
            prevTime = candlestick.getTimestamp();
        }
        return getResultData(fullData, timeFrame);
    }

    public TradingDataOutput getForTimeFrame(int toTimestamp, int limit, DexCurrency currency, TimeFrame timeFrame) {
        int startTime = toTimestamp - limit * timeFrame.muliplier * BASE_TIME_INTERVAL;
        DexCandlestick last = candlestickDao.getLast(currency);
        int timestamp = last.getTimestamp();
        int timeInterval = timeFrame.muliplier * BASE_TIME_INTERVAL;
        int orderStartTime = timestamp + timeInterval;
        List<SimpleTradingEntry> data = new ArrayList<>();
        if (orderStartTime < startTime) {
            data.addAll(getForTimeFrameFromDexOrders(startTime, toTimestamp, currency, timeFrame));
        } else {
            List<SimpleTradingEntry> dexOrderCandlesticks = getForTimeFrameFromDexOrders(orderStartTime, toTimestamp, currency, timeFrame);
            limit -= dexOrderCandlesticks.size();
            toTimestamp = orderStartTime;
            data.addAll(getForTimeFrameFromCandlesticks(toTimestamp, limit, currency, timeFrame));
            data.addAll(dexOrderCandlesticks);
        }
        return buildTradingDataOutput(toTimestamp, startTime, data);
    }
    public List<SimpleTradingEntry> getForTimeFrameFromDexOrders(int fromTimestamp, int toTimestamp, DexCurrency currency, TimeFrame timeFrame) {
        List<DexCandlestick> orderCandlesticks = getOrderCandlesticks(fromTimestamp, toTimestamp, currency);
        int prevTime = fromTimestamp;
        List<SimpleTradingEntry> fullData = new ArrayList<>();
        for (DexCandlestick candlestick : orderCandlesticks) {
            pushUntil(prevTime, candlestick.getTimestamp(), fullData);
            fullData.add(fromCandleStick(candlestick));
            prevTime = candlestick.getTimestamp();
        }
        return getResultData(fullData, timeFrame);
    }

    private List<DexCandlestick> getOrderCandlesticks(int fromTimestamp, int toTimestamp, DexCurrency currency) {
        int fromEpochTime = Convert2.toEpochTime(fromTimestamp);
        int toEpochTime = Convert2.toEpochTime(toTimestamp);
        long fromDbId = 0;
        List<DexOrder> orders;
        Map<Integer, DexCandlestick> candlesticks = new HashMap<>();
        do {
            orders = orderDao.getOrdersFromDbIdBetweenTimestamps(OrderDbIdPaginationDbRequest.builder()
                    .limit(100)
                    .coin(currency)
                    .fromTime(fromEpochTime)
                    .toTime(toEpochTime)
                    .fromDbId(fromDbId)
                    .build());
            DexTradingGraphScanningService.convertOrders(orders, candlesticks);
            if (orders.size() > 0) {
                fromDbId = orders.get(orders.size() - 1).getDbId();
            }
        } while (orders.size() == 100);
        return candlesticks.values().stream().sorted(Comparator.comparing(DexCandlestick::getTimestamp)).collect(Collectors.toList());
    }


    private List<SimpleTradingEntry> getResultData(List<SimpleTradingEntry> fullData, TimeFrame timeFrame) {
        if (timeFrame != TimeFrame.QUARTER) {
            return groupBy(fullData, timeFrame);
        }
        return fullData;
    }

    private TradingDataOutput buildTradingDataOutput(int toTimestamp, int fromTimestamp, List<SimpleTradingEntry> data) {
        TradingDataOutput tradingDataOutput = new TradingDataOutput();
        tradingDataOutput.setResponse("Success");
        tradingDataOutput.setType(100);
        tradingDataOutput.setAggregated(false);
        tradingDataOutput.setData(data);
        tradingDataOutput.setTimeTo(toTimestamp);
        tradingDataOutput.setTimeFrom(fromTimestamp);
        tradingDataOutput.setFirstValueInArray(true);
        ConversionType conversionType = new ConversionType();
        conversionType.type = "force_direct";
        conversionType.conversionSymbol = "";
        tradingDataOutput.setConversionType(conversionType);
        tradingDataOutput.setHasWarning(false);
        return tradingDataOutput;
    }

    private List<SimpleTradingEntry> groupBy(List<SimpleTradingEntry> entries, TimeFrame tf) {
        if (entries.size() % tf.muliplier != 0) {
            throw new IllegalArgumentException("Got " + entries.size() + " trading entries, expected size%" + tf.muliplier + "=0");
        }
        int groupedDataSize = entries.size() / tf.muliplier;
        List<SimpleTradingEntry> groupedData = new ArrayList<>(groupedDataSize);
        for (int i = 0; i < groupedDataSize; i++) {
            List<SimpleTradingEntry> listToGroup = entries.subList(i * tf.muliplier, (i + 1) * tf.muliplier);
            SimpleTradingEntry compressed = compress(listToGroup);
            groupedData.add(compressed);
        }
        return groupedData;
    }

    private SimpleTradingEntry compress(List<SimpleTradingEntry> entries) {
        BigDecimal totalVolumeFrom = BigDecimal.ZERO;
        BigDecimal totalVolumeTo = BigDecimal.ZERO;
        BigDecimal maxPrice = BigDecimal.ZERO;
        BigDecimal openPrice = BigDecimal.ZERO;
        BigDecimal closePrice = BigDecimal.ZERO;
        BigDecimal minPrice = BigDecimal.valueOf(Long.MAX_VALUE);
        boolean openFound = false;
        boolean closeFound = false;
        for (SimpleTradingEntry entry : entries) {
            if (maxPrice.compareTo(entry.getHigh()) < 0) {
                maxPrice = entry.getHigh();
            }
            if (minPrice.compareTo(entry.getLow()) > 0) {
                minPrice = entry.getLow();
            }
            totalVolumeFrom = totalVolumeFrom.add(entry.getVolumefrom());
            totalVolumeTo = totalVolumeTo.add(entry.getVolumeto());
            if (!openFound && entry.getOpen() != null) {
                openPrice = entry.getOpen();
                openFound = true;
            }
            if (!closeFound && entry.getClose() != null) {
                closePrice = entry.getClose();
                closeFound = true;
            }
        }
        return new SimpleTradingEntry(entries.get(0).getTime(), openPrice, closePrice, minPrice, maxPrice, totalVolumeFrom, totalVolumeTo);

    }

    private SimpleTradingEntry fromCandleStick(DexCandlestick c) {
        return new SimpleTradingEntry(c.getTimestamp(), c.getOpen(), c.getClose(), c.getMin(), c.getMax(), c.getFromVolume(), c.getToVolume());
    }

    private void pushUntil(int startTimestamp, int finishTimestamp, List<SimpleTradingEntry> entries) {
        for (int currentTimestamp = startTimestamp; currentTimestamp < finishTimestamp; currentTimestamp+= BASE_TIME_INTERVAL) {
            entries.add(emptyEntry(currentTimestamp));
        }
    }

    private SimpleTradingEntry emptyEntry(int timestamp) {
        SimpleTradingEntry entry = new SimpleTradingEntry();
        entry.setTime(timestamp);
        return entry;
    }

}
