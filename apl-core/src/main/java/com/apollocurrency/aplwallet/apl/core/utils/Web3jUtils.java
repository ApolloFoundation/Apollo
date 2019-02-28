package com.apollocurrency.aplwallet.apl.core.utils;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Web3jUtils {

    // see https://www.reddit.com/r/ethereum/comments/5g8ia6/attention_miners_we_recommend_raising_gas_limit/
    public static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);

    // http://ethereum.stackexchange.com/questions/1832/cant-send-transaction-exceeds-block-gas-limit-or-intrinsic-gas-too-low
    public static final BigInteger GAS_LIMIT_ETHER_TX = BigInteger.valueOf(21_000);
    public static final BigInteger GAS_LIMIT_GREETER_TX = BigInteger.valueOf(500_000L);

    public static final int CONFIRMATION_ATTEMPTS = 40;
    public static final int SLEEP_DURATION = 1000;


    public static BigDecimal weiToEther(BigInteger wei) {
        return Convert.fromWei(wei.toString(), Convert.Unit.ETHER);
    }

    public static BigInteger etherToWei(BigDecimal ether) {
        return Convert.toWei(ether, Convert.Unit.ETHER).toBigInteger();
    }

}
