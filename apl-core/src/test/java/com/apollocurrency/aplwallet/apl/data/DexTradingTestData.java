package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexCurrency;

import java.math.BigDecimal;

public class DexTradingTestData {
    public final DexCandlestick ETH_0_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000032"), dec("0.0000041"), dec("0.0000034"), dec("0.0000039 "), dec("1559000.00"), dec("1114.9034283"), 1574851500, 1574851510, 1574851510);
    public final DexCandlestick ETH_1_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000037"), dec("0.0000040"), dec("0.0000039"), dec("0.0000040"), dec("978000.00"), dec("651.4532560"), 1574852400, 1574852500, 1574852600);
    public final DexCandlestick PAX_0_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2), dec("1.3000000"), dec("1.5300000"), dec("1.4400000"), dec("1.4100000"), dec("1200.50"), dec("1805.2100000"), 1574852400, 1574852420, 1574852670);
    public final DexCandlestick ETH_2_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000036"), dec("0.0000043"), dec("0.0000040"), dec("0.0000037"), dec("1199000.00"), dec("812.8812000"), 1574853300, 1574853350, 1574853820);
    public final DexCandlestick PAX_1_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2), dec("1.3900000"), dec("1.4800000"), dec("1.4100000"), dec("1.4700000"), dec("521.20"), dec("712.8900000"), 1574853300, 1574853330, 1574853350);
    public final DexCandlestick ETH_3_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000031"), dec("0.0000034"), dec("0.0000032"), dec("0.0000034"), dec("270800.00"), dec("115.1346320"), 1574855100, 1574855350, 1574855350);
    public final DexCandlestick ETH_4_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000035"), dec("0.0000035"), dec("0.0000035"), dec("0.0000035"), dec("100000.00"), dec("61.3265230"), 1574857800, 1574857900, 1574858699);
    public final DexCandlestick ETH_5_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000031"), dec("0.0000036"), dec("0.0000035"), dec("0.0000033"), dec("845246.00"), dec("575.4636220"), 1574858700, 1574858720, 1574858999);
    public final DexCandlestick PAX_2_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2), dec("1.3800000"), dec("1.4500000"), dec("1.4000000"), dec("1.4200000"), dec("401.25"), dec("555.7000000"), 1574858700, 1574858701, 1574859500);
    public final DexCandlestick ETH_6_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000032"), dec("0.0000036"), dec("0.0000033"), dec("0.0000034"), dec("1288000.00"), dec("897.3623660"), 1574859600, 1574859650, 1574859810);
    public final DexCandlestick ETH_7_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000028"), dec("0.0000034"), dec("0.0000034"), dec("0.0000029"), dec("1733000.00"), dec("1123.3423660"), 1574860500, 1574861200, 1574861200);
    public final DexCandlestick ETH_8_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000025"), dec("0.0000034"), dec("0.0000029"), dec("0.0000031"), dec("443000.00"), dec("233.4633200"), 1574861400, 1574861450, 1574861999);
    public final DexCandlestick PAX_3_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2), dec("1.300000"), dec("1.3800000"), dec("1.3800000"), dec("1.3200000"), dec("257.50"), dec("342.9000000"), 1574861400, 1574861410, 1574862200);
    public final DexCandlestick PAX_4_CANDLESTICK = new DexCandlestick(DexCurrency.getType(2), dec("1.2200000"), dec("1.3100000"), dec("1.3000000"), dec("1.2500000"), dec("432.00"), dec("545.3000000"), 1574863200, 1574863300, 1574863500);
    public final DexCandlestick ETH_9_CANDLESTICK = new DexCandlestick(DexCurrency.getType(1), dec("0.0000032"), dec("0.0000041"), dec("0.0000034"), dec("0.0000039"), dec("1807000.00"), dec("1350.4354230"), 1574865900, 1574866300, 1574866500);

    private static BigDecimal dec(String d) {
        return new BigDecimal(d.trim());
    }

}
