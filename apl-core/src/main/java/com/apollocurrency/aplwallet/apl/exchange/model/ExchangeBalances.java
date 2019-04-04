package com.apollocurrency.aplwallet.apl.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.json.simple.JSONObject;

import java.math.BigInteger;

public class ExchangeBalances {

    private BigInteger balanceETH;
    private BigInteger balancePAX;
    private BigInteger balanceBTC;


    /**
     * Ethereum account balance in wei
     **/

    @ApiModelProperty(value = "Ethereum account balance in wei")
    @JsonProperty("balanceETH")
    public BigInteger getBalanceETH() {
        return balanceETH;
    }
    public void setBalanceETH(BigInteger balanceETH) {
        this.balanceETH = balanceETH;
    }

    /**
     * Bitcoin balance in satoshi
     **/

    @ApiModelProperty(value = "Bitcoin balance in satoshi")
    @JsonProperty("balanceBTC")
    public BigInteger getBalanceBTC() {
        return balanceBTC;
    }
    public void setBalanceBTC(BigInteger balanceBTC) {
        this.balanceBTC = balanceBTC;
    }

    public BigInteger getBalancePAX() {
        return balancePAX;
    }

    public void setBalancePAX(BigInteger balancePAX) {
        this.balancePAX = balancePAX;
    }

    public JSONObject balanceToJson() {
        JSONObject json = new JSONObject();

        json.put("balanceETH", String.valueOf(getBalanceETH()));
        json.put("balancePAX", String.valueOf(getBalancePAX()));

        return json;
    }
}
