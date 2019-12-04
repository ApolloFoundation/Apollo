package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.api.trading.ConversionType;
import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DexTradingDataService {
    private boolean enableTradingViewGraphDataFeeder;
    private boolean enableTradingDataCache;
    private DexCandlestickDao dao;
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
                                 DexCandlestickDao dao) {
        this.enableTradingViewGraphDataFeeder = enableTradingViewGraphDataFeeder;
        this.enableTradingDataCache = enableTradingDataCache;
        this.dao = dao;
    }

    public TradingDataOutput getForTimeFrame(int toTimestamp, int limit, DexCurrency currency, TimeFrame timeFrame) {
        int startTime = toTimestamp - limit * timeFrame.muliplier * BASE_TIME_INTERVAL;
        List<DexCandlestick> candlesticks = dao.getFromToTimestamp(startTime, toTimestamp, currency);
        List<SimpleTradingEntry> fullData = new ArrayList<>(limit * timeFrame.muliplier);
        int prevTime = startTime;
        for (DexCandlestick candlestick : candlesticks) {
            pushUntil(prevTime, candlestick.getTimestamp(), fullData);
            fullData.add(fromCandleStick(candlestick));
            prevTime = candlestick.getTimestamp();
        }
        List<SimpleTradingEntry> resultData = getResultData(fullData, timeFrame);
        return buildTradingDataOutput(toTimestamp, startTime, resultData);
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
