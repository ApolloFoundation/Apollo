package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import jnr.a64asm.OP;

import java.math.BigDecimal;

public class DexTestData {
    public final long ALICE = 100;
    public final long BOB = 200;
    // type(Buy/Sell currency (ETH/PAX) account (Alice/BOB)
    public final DexOrder ORDER_BEA_1 = new DexOrder(1000L, 1L, OrderType.BUY, 100L, DexCurrencies.APL, 500000L, DexCurrencies.ETH, BigDecimal.valueOf(0.001), 6000, OrderStatus.CLOSED, 100, "0x602242c68640e754677b683e20a2740f8f95f7d3", "APL-K78W-Z7LR-TPJY-73HZK");
    public final DexOrder ORDER_SPA_2 = new DexOrder(1010L, 2L, OrderType.SELL, 100L, DexCurrencies.APL, 200000L, DexCurrencies.PAX, BigDecimal.valueOf(0.16), 6500, OrderStatus.CANCEL, 110, "APL-K78W-Z7LR-TPJY-73HZK", "0x602242c68640e754677b683e20a2740f8f95f7d3");
    public final DexOrder ORDER_BPB_1 = new DexOrder(1020L, 3L, OrderType.BUY, 200L, DexCurrencies.APL, 100000L, DexCurrencies.PAX, BigDecimal.valueOf(0.15), 7000, OrderStatus.OPEN, 121, "0x777BE94ea170AfD894Dd58e9634E442F6C5602EF", "APL-T69E-CTDG-8TYM-DKB5H");
    public final DexOrder ORDER_SEA_3 = new DexOrder(1030L, 4L, OrderType.SELL, 100L, DexCurrencies.APL, 400000L, DexCurrencies.ETH, BigDecimal.valueOf(0.001), 8000, OrderStatus.WAITING_APPROVAL, 121, "APL-K78W-Z7LR-TPJY-73HZK", "0x602242c68640e754677b683e20a2740f8f95f7d3");
    public final DexOrder ORDER_BEA_4 = new DexOrder(1040L, 5L, OrderType.BUY, 100L, DexCurrencies.APL, 600000L, DexCurrencies.ETH, BigDecimal.valueOf(0.001), 11000, OrderStatus.OPEN, 122, "0x602242c68640e754677b683e20a2740f8f95f7d3", "APL-K78W-Z7LR-TPJY-73HZK");
}
