package com.apollocurrency.aplwallet.apl.exchange.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.Objects;


public class ExchangeOrder {

    private BigDecimal orderId = null;
    private String orderType = null;
    private String pair = null;
    private BigDecimal rate = null;
    private BigDecimal amountAPL = null;
    private BigDecimal amount = null;
    private String status = null;
    private String openTime = null;
    private BigDecimal transaction = null;
    private BigDecimal execTransaction = null;

    /**
     * Order Id
     **/

    @ApiModelProperty(value = "Order Id")
    @JsonProperty("orderId")
    public BigDecimal getOrderId() {
        return orderId;
    }

    public void setOrderId(BigDecimal orderId) {
        this.orderId = orderId;
    }

    /**
     * Type of pending order - buy or sell
     **/

    @ApiModelProperty(value = "Type of pending order - buy or sell")
    @JsonProperty("orderType")
    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    /**
     * Currency Pair (exchange direction) (APLBTC, APLETH)
     **/

    @ApiModelProperty(value = "Currency Pair (exchange direction) (APLBTC, APLETH)")
    @JsonProperty("pair")
    public String getPair() {
        return pair;
    }

    public void setPair(String pair) {
        this.pair = pair;
    }

    /**
     * Exchange rate
     **/

    @ApiModelProperty(value = "Exchange rate")
    @JsonProperty("rate")
    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    /**
     * Amount of APL to exchange
     **/

    @ApiModelProperty(value = "Amount of APL to exchange")
    @JsonProperty("amountAPL")
    public BigDecimal getAmountAPL() {
        return amountAPL;
    }

    public void setAmountAPL(BigDecimal amountAPL) {
        this.amountAPL = amountAPL;
    }

    /**
     * Amount of Satoshi, wei, or PAX to exchange
     **/

    @ApiModelProperty(value = "Amount of Satoshi, wei, or PAX to exchange")
    @JsonProperty("amount")
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Order status from enum - [OPENED, EXECUTED, CANCELLED etc...]
     **/

    @ApiModelProperty(value = "Order status from enum - [OPENED, EXECUTED, CANCELLED etc...]")
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Order placement time
     **/

    @ApiModelProperty(value = "Order placement time")
    @JsonProperty("openTime")
    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    /**
     * Order Placement transaction id
     **/

    @ApiModelProperty(value = "Order Placement transaction id")
    @JsonProperty("transaction")
    public BigDecimal getTransaction() {
        return transaction;
    }

    public void setTransaction(BigDecimal transaction) {
        this.transaction = transaction;
    }

    /**
     * Order execution/cancellation transaction id
     **/

    @ApiModelProperty(value = "Order execution/cancellation transaction id")
    @JsonProperty("execTransaction")
    public BigDecimal getExecTransaction() {
        return execTransaction;
    }

    public void setExecTransaction(BigDecimal execTransaction) {
        this.execTransaction = execTransaction;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExchangeOrder orders = (ExchangeOrder) o;
        return Objects.equals(orderId, orders.orderId) &&
            Objects.equals(orderType, orders.orderType) &&
            Objects.equals(pair, orders.pair) &&
            Objects.equals(rate, orders.rate) &&
            Objects.equals(amountAPL, orders.amountAPL) &&
            Objects.equals(amount, orders.amount) &&
            Objects.equals(status, orders.status) &&
            Objects.equals(openTime, orders.openTime) &&
            Objects.equals(transaction, orders.transaction) &&
            Objects.equals(execTransaction, orders.execTransaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, orderType, pair, rate, amountAPL, amount, status, openTime, transaction, execTransaction);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Orders {\n");

        sb.append("    orderId: ").append(toIndentedString(orderId)).append("\n");
        sb.append("    orderType: ").append(toIndentedString(orderType)).append("\n");
        sb.append("    pair: ").append(toIndentedString(pair)).append("\n");
        sb.append("    rate: ").append(toIndentedString(rate)).append("\n");
        sb.append("    amountAPL: ").append(toIndentedString(amountAPL)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    openTime: ").append(toIndentedString(openTime)).append("\n");
        sb.append("    transaction: ").append(toIndentedString(transaction)).append("\n");
        sb.append("    execTransaction: ").append(toIndentedString(execTransaction)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
