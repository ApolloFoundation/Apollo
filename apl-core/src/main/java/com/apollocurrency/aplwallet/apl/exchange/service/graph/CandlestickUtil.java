package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;

import javax.enterprise.inject.Vetoed;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Vetoed
public class CandlestickUtil {
    static final int BASE_TIME_INTERVAL = 15 * 60; // 15 minutes in seconds

    private CandlestickUtil() {
    }

    public static void convertOrders(List<DexOrder> orders, Map<Integer, DexCandlestick> candlesticks, TimeFrame frame, CandlestickLoader loader) {
        for (DexOrder order : orders) {
            int unixEpochSeconds = (int) (Convert2.fromEpochTime(order.getFinishTime()) / 1000);
            int openTime = unixEpochSeconds - unixEpochSeconds % (BASE_TIME_INTERVAL * frame.muliplier);
            DexCandlestick dexCandlestick = candlesticks.get(openTime);
            if (dexCandlestick == null) {
                dexCandlestick = loader.load(openTime);
            }
            candlesticks.put(openTime, toCandlestick(order, dexCandlestick, openTime, unixEpochSeconds));
        }
    }

    private static DexCandlestick toCandlestick(DexOrder order, DexCandlestick thisCandlestick, int candlestickTime, int orderUnixFinishTime) {
        BigDecimal aplAmount = EthUtil.atmToEth(order.getOrderAmount());
        if (thisCandlestick != null) {
            thisCandlestick.setFromVolume(thisCandlestick.getFromVolume().add(aplAmount));
            BigDecimal pairedCurrencyVolume = order.getPairRate().multiply(aplAmount);
            thisCandlestick.setToVolume(thisCandlestick.getToVolume().add(pairedCurrencyVolume));
            if (thisCandlestick.getCloseOrderTimestamp() < order.getFinishTime()) {
                thisCandlestick.setClose(order.getPairRate());
                thisCandlestick.setCloseOrderTimestamp(order.getFinishTime());
            }
            if (thisCandlestick.getOpenOrderTimestamp() > order.getFinishTime()) {
                thisCandlestick.setOpen(order.getPairRate());
                thisCandlestick.setOpenOrderTimestamp(order.getFinishTime());
            }
            if (thisCandlestick.getMax().compareTo(order.getPairRate()) < 0) {
                thisCandlestick.setMax(order.getPairRate());
            }
            if (thisCandlestick.getMin().compareTo(order.getPairRate()) > 0) {
                thisCandlestick.setMin(order.getPairRate());
            }
            return thisCandlestick;
        } else {
            return new DexCandlestick(order.getPairCurrency(), order.getPairRate(), order.getPairRate(), order.getPairRate(),
                order.getPairRate(), aplAmount, order.getPairRate().multiply(aplAmount), candlestickTime, orderUnixFinishTime, orderUnixFinishTime);
        }
    }

}
