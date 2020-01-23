package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.util.Constants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CandlestickTestUtil {
    public static long apl(long atm) {
        return atm * Constants.ONE_APL;
    }

    public static SimpleTradingEntry fromRawData(String min, String max, String open, String close, String volumeFrom, String volumeTo, int timestamp) {
        return new SimpleTradingEntry(timestamp, dec(open), dec(close), dec(min), dec(max), dec(volumeFrom), dec(volumeTo));
    }

    public static BigDecimal dec(String s) {
        return new BigDecimal(s.trim());
    }


    public static SimpleTradingEntry empty(int time) {
        return new SimpleTradingEntry(time, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static SimpleTradingEntry fromCandlestick(DexCandlestick candlestick) {
        return new SimpleTradingEntry(candlestick.getTimestamp(), candlestick.getOpen(), candlestick.getClose(), candlestick.getMin(), candlestick.getMax(), candlestick.getFromVolume(), candlestick.getToVolume());
    }


    public static DexCandlestick eCandlestick(String min, String max, String open, String close, String volumeFrom, String volumeTo, int timestamp, int opeTime, int closeTime) {
        return new DexCandlestick(DexCurrency.ETH, dec(min), dec(max), dec(open), dec(close), dec(volumeFrom), dec(volumeTo), timestamp, opeTime, closeTime);
    }
    public static DexCandlestick pCandlestick(String min, String max, String open, String close, String volumeFrom, String volumeTo, int timestamp, int opeTime, int closeTime) {
        return new DexCandlestick(DexCurrency.PAX, dec(min), dec(max), dec(open), dec(close), dec(volumeFrom), dec(volumeTo), timestamp, opeTime, closeTime);
    }

    public static DexOrder eOrder(long dbId, int finishTime, BigDecimal pairRate, long amount) {
        return new DexOrder(dbId, 1L, OrderType.BUY, 1L, DexCurrency.APL, amount, DexCurrency.ETH, pairRate, finishTime, OrderStatus.CLOSED, 100, null, null);
    }
    public static DexOrder eOrder(long dbId, int finishTime, BigDecimal pairRate, long amount, int height) {
        return new DexOrder(dbId, 1L, OrderType.BUY, 1L, DexCurrency.APL, amount, DexCurrency.ETH, pairRate, finishTime, OrderStatus.CLOSED, height, null, null);
    }
    public static DexOrder pOrder(long dbId, int finishTime, BigDecimal pairRate, long amount, int height) {
        return new DexOrder(dbId, 1L, OrderType.BUY, 1L, DexCurrency.APL, amount, DexCurrency.PAX, pairRate, finishTime, OrderStatus.CLOSED, height, null, null);
    }

    public static List<SimpleTradingEntry> fromTable(TradingDataOutput table) {
        List<SimpleTradingEntry> entries = new ArrayList<>();
        for (int i = 0; i < table.getC().size(); i++) {
            entries.add(new SimpleTradingEntry(table.getT().get(i), table.getO().get(i), table.getC().get(i), table.getL().get(i), table.getH().get(i), table.getV().get(i), null));
        }
        return entries;
    }

    private CandlestickTestUtil() {}
}
