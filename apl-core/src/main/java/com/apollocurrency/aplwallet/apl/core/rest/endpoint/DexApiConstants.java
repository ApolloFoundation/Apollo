package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class DexApiConstants {
    public static final String WALLET_ADDRESS = "walletAddress";
    public static final String PAIR_CURRENCY = "pairCurrency";
    public static final String COUNTER_ORDER_ID = "counterOrderId";
    public static final String ORDER_ID = "orderId";

    private DexApiConstants() {}
}
