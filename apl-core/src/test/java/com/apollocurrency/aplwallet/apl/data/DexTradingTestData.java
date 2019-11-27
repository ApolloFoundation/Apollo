package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;

import java.math.BigDecimal;

public class DexTradingTestData {
    public final DexCandlestick ETH_0_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000032")    ,dec("0.0000041") ,dec("0.0000034")   ,dec("0.0000039 ") ,dec("1559000")          , dec("1114.9034283" ) , 1574851000);
    public final DexCandlestick ETH_1_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000037")    ,dec("0.000004 ") ,dec("0.0000039")   ,dec("0.000004  ") ,dec("978000 ")          , dec("651.453256  " ) , 1574851900);
    public final DexCandlestick PAX_0_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2)     ,dec("1.3      ")    ,dec("1.53     ") ,dec("1.44     ")   ,dec("1.41      ") ,dec("1200.5 ")          , dec("1805.21     " ) , 1574851900);
    public final DexCandlestick ETH_2_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000036")    ,dec("0.0000043") ,dec("0.000004 ")   ,dec("0.0000037 ") ,dec("1199000")          , dec("812.8812    " ) , 1574852800);
    public final DexCandlestick PAX_1_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2)     ,dec("1.39     ")    ,dec("1.48     ") ,dec("1.41     ")   ,dec("1.47      ") ,dec("521.2  ")          , dec("712.89      " ) , 1574852800);
    public final DexCandlestick ETH_3_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000031")    ,dec("0.0000034") ,dec("0.0000032")   ,dec("0.0000034 ") ,dec("270800 ")          , dec("115.134632  " ) , 1574854600);
    public final DexCandlestick ETH_4_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000035")    ,dec("0.0000035") ,dec("0.0000035")   ,dec("0.0000035 ") ,dec("100000 ")          , dec("61.326523   " ) , 1574857300);
    public final DexCandlestick ETH_5_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000031")    ,dec("0.0000036") ,dec("0.0000035")   ,dec("0.0000033 ") ,dec("845246 ")          , dec("575.463622  " ) , 1574858200);
    public final DexCandlestick PAX_2_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2)     ,dec("1.38     ")    ,dec("1.45     ") ,dec("1.4      ")   ,dec("1.42      ") ,dec("401.25 ")          , dec("555.7       " ) , 1574858200);
    public final DexCandlestick ETH_6_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000032")    ,dec("0.0000036") ,dec("0.0000033")   ,dec("0.0000034 ") ,dec("1288000")          , dec("897.362366  " ) , 1574859100);
    public final DexCandlestick ETH_7_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000028")    ,dec("0.0000034") ,dec("0.0000034")   ,dec("0.0000029 ") ,dec("1733000")          , dec("1123.342366 " ) , 1574860000);
    public final DexCandlestick ETH_8_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000025")    ,dec("0.0000034") ,dec("0.0000029")   ,dec("0.0000031 ") ,dec("443000 ")          , dec("233.46332   " ) , 1574860900);
    public final DexCandlestick PAX_3_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2)     ,dec("1.3      ")    ,dec("1.38     ") ,dec("1.38     ")   ,dec("1.32      ") ,dec("257.5  ")          , dec("342.9       " ) , 1574860900);
    public final DexCandlestick PAX_4_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2)     ,dec("1.22     ")    ,dec("1.31     ") ,dec("1.3      ")   ,dec("1.25      ") ,dec("432    ")          , dec("545.3       " ) , 1574862700);
    public final DexCandlestick ETH_9_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1)     ,dec("0.0000032")    ,dec("0.0000041") ,dec("0.0000034")   ,dec("0.0000039 ") ,dec("1807000")          , dec("1350.435423 " ) , 1574865400);

    private static BigDecimal dec(String d) {
        return new BigDecimal(d.trim());
    }

}
