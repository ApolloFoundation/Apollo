package com.apollocurrency.aplwallet.apl.dex.eth.model;

import java.math.BigInteger;

public enum EthUnit {
    WEI(BigInteger.valueOf(1L)),
    GWEI(BigInteger.valueOf(1000000000L)),
    SZABO(BigInteger.valueOf(1000000000000L)),
    FINNEY(BigInteger.valueOf(1000000000000000L)),
    ETHER(BigInteger.valueOf(1000000000000000000L));

    public BigInteger i;

    private EthUnit(BigInteger i) {
        this.i = i;
    }
}
