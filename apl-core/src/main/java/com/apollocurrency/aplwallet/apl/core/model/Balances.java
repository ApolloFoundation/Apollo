package com.apollocurrency.aplwallet.apl.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.json.simple.JSONObject;

import java.util.Objects;

public class Balances {

    private String accountRS;
    private long account;
    private long balanceATM;
    private long unconfirmedBalanceATM;
    private long forgedBalanceATM;
    private long effectiveBalanceAPL;
    private long guaranteedBalanceATM;


//    private BigInteger balanceETH = null;
//    private BigInteger balancePAX = null;
//    private BigInteger balanceBTC = null;

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
    public long getAccount() {
        return account;
    }
    public void setAccount(long account) {
        this.account = account;
    }

    /**
     * Apollo account balance in Atoms
     **/

    @ApiModelProperty(value = "Apollo account balance in Atoms")
    @JsonProperty("balanceATM")
    public long getBalanceATM() {
        return balanceATM;
    }
    public void setBalanceATM(long balanceATM) {
        this.balanceATM = balanceATM;
    }

    public long getUnconfirmedBalanceATM() {
        return unconfirmedBalanceATM;
    }

    public void setUnconfirmedBalanceATM(long unconfirmedBalanceATM) {
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
    }

    public long getForgedBalanceATM() {
        return forgedBalanceATM;
    }

    public void setForgedBalanceATM(long forgedBalanceATM) {
        this.forgedBalanceATM = forgedBalanceATM;
    }

    public long getEffectiveBalanceAPL() {
        return effectiveBalanceAPL;
    }

    public void setEffectiveBalanceAPL(long effectiveBalanceAPL) {
        this.effectiveBalanceAPL = effectiveBalanceAPL;
    }

    public long getGuaranteedBalanceATM() {
        return guaranteedBalanceATM;
    }

    public void setGuaranteedBalanceATM(long guaranteedBalanceATM) {
        this.guaranteedBalanceATM = guaranteedBalanceATM;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Balances balances = (Balances) o;
        return account == balances.account &&
                balanceATM == balances.balanceATM &&
                unconfirmedBalanceATM == balances.unconfirmedBalanceATM &&
                forgedBalanceATM == balances.forgedBalanceATM &&
                effectiveBalanceAPL == balances.effectiveBalanceAPL &&
                guaranteedBalanceATM == balances.guaranteedBalanceATM &&
                Objects.equals(accountRS, balances.accountRS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountRS, account, balanceATM, unconfirmedBalanceATM, forgedBalanceATM, effectiveBalanceAPL, guaranteedBalanceATM);
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


    public JSONObject balanceToJson() {
        JSONObject json = new JSONObject();

        json.put("balanceATM", String.valueOf(getBalanceATM()));
        json.put("unconfirmedBalanceATM", String.valueOf(getUnconfirmedBalanceATM()));
        json.put("forgedBalanceATM", String.valueOf(getForgedBalanceATM()));
        json.put("effectiveBalanceAPL", getEffectiveBalanceAPL());
        json.put("guaranteedBalanceATM", String.valueOf(getGuaranteedBalanceATM()));

        return json;
    }
}


