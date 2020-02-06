package com.apollocurrency.aplwallet.apl.eth.model;

import com.apollocurrency.aplwallet.apl.core.config.EthBalanceWeiToEthSerializer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class EthWalletBalanceInfo {

    private String address;

    private Map<String, BigInteger> balances = new HashMap<>();

    public EthWalletBalanceInfo(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void put(String currency, BigInteger balance) {
        balances.put(currency, balance);
    }

    @JsonSerialize(contentUsing = EthBalanceWeiToEthSerializer.class)
    public Map<String, BigInteger> getBalances() {
        return balances;
    }


    public BigInteger getEth() {
        return balances.get(DexCurrency.ETH.getCurrencyCode());
    }

    public BigInteger getPax() {
        return balances.get(DexCurrency.PAX.getCurrencyCode());
    }
}
