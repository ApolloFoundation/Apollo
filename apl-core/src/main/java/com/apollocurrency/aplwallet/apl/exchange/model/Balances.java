package com.apollocurrency.aplwallet.apl.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class Balances {


    private String accountRS = null;
    private BigDecimal account = null;
    private BigDecimal balanceATM = null;
    private BigDecimal balanceETH = null;
    private BigDecimal balanceBTC = null;

    /**
     * Account RS
     **/

    @ApiModelProperty(value = "Account RS")
    @JsonProperty("accountRS")
    public String getAccountRS() {
        return accountRS;
    }
    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    /**
     * Account Number
     **/

    @ApiModelProperty(value = "Account Number")
    @JsonProperty("account")
    public BigDecimal getAccount() {
        return account;
    }
    public void setAccount(BigDecimal account) {
        this.account = account;
    }

    /**
     * Apollo account balance in Atoms
     **/

    @ApiModelProperty(value = "Apollo account balance in Atoms")
    @JsonProperty("balanceATM")
    public BigDecimal getBalanceATM() {
        return balanceATM;
    }
    public void setBalanceATM(BigDecimal balanceATM) {
        this.balanceATM = balanceATM;
    }

    /**
     * Ethereum account balance in wei
     **/

    @ApiModelProperty(value = "Ethereum account balance in wei")
    @JsonProperty("balanceETH")
    public BigDecimal getBalanceETH() {
        return balanceETH;
    }
    public void setBalanceETH(BigDecimal balanceETH) {
        this.balanceETH = balanceETH;
    }

    /**
     * Bitcoin balance in satoshi
     **/

    @ApiModelProperty(value = "Bitcoin balance in satoshi")
    @JsonProperty("balanceBTC")
    public BigDecimal getBalanceBTC() {
        return balanceBTC;
    }
    public void setBalanceBTC(BigDecimal balanceBTC) {
        this.balanceBTC = balanceBTC;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Balances balances = (Balances) o;
        return Objects.equals(accountRS, balances.accountRS) &&
                Objects.equals(account, balances.account) &&
                Objects.equals(balanceATM, balances.balanceATM) &&
                Objects.equals(balanceETH, balances.balanceETH) &&
                Objects.equals(balanceBTC, balances.balanceBTC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountRS, account, balanceATM, balanceETH, balanceBTC);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Balances {\n");

        sb.append("    accountRS: ").append(toIndentedString(accountRS)).append("\n");
        sb.append("    account: ").append(toIndentedString(account)).append("\n");
        sb.append("    balanceATM: ").append(toIndentedString(balanceATM)).append("\n");
        sb.append("    balanceETH: ").append(toIndentedString(balanceETH)).append("\n");
        sb.append("    balanceBTC: ").append(toIndentedString(balanceBTC)).append("\n");
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
