package com.apollocurrency.aplwallet.apl.eth.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EthUtilTest {

    private BigInteger WEI = new BigInteger("1234567890000000000");
    private Long GWEI = 1234567890L;
    private Long ATM = 123456789L;
    private BigDecimal ETH = new BigDecimal("1.23456789");

    @Test
    void weiToEther() {
        BigDecimal eth = EthUtil.weiToEther(WEI);
        assertEquals(ETH, eth);
    }

    @Test
    void ethToGwei() {
        Long gwei = EthUtil.ethToGwei(ETH);
        assertEquals(GWEI, gwei);
    }

    @Test
    void etherToWei() {
        BigInteger wei = EthUtil.etherToWei(ETH);
        assertEquals(WEI, wei);
    }

    @Test
    void gweiToWei() {
        BigInteger wei = EthUtil.gweiToWei(GWEI);
        assertEquals(WEI, wei);
    }

    @Test
    void gweiToEth() {
        BigDecimal eth = EthUtil.gweiToEth(GWEI);
        assertEquals(ETH, eth);
    }

    @Test
    void atmToEth() {
        BigDecimal eth = EthUtil.atmToEth(ATM);
        assertEquals(ETH, eth);
    }

    @Test
    void gweiToAtm() {
        Long apl = EthUtil.gweiToAtm(GWEI);
        assertEquals(ATM, apl);
    }
}