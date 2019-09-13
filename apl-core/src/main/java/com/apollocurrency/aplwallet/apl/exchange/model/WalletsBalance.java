package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.eth.model.EthWalletBalanceInfo;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class WalletsBalance {

    @JsonProperty("eth")
    private List<EthWalletBalanceInfo> ethWalletsBalance;

    public WalletsBalance(List<EthWalletBalanceInfo> ethWalletsBalance) {
        this.ethWalletsBalance = ethWalletsBalance;
    }

    public List<EthWalletBalanceInfo> getEthWalletsBalance() {
        return ethWalletsBalance;
    }

    public void setEthWalletsBalance(List<EthWalletBalanceInfo> ethWalletsBalance) {
        this.ethWalletsBalance = ethWalletsBalance;
    }
}
