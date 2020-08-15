package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class AccountInfo   {
  private String account = null;  private String publicKey = null;  private String parent = null;  private String balance = null;  private String unconfirmedbalance = null;  /**
   * Gets or Sets status
   */
  public enum StatusEnum {
    CREATED("created"),
    VERIFIED("verified");
    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }
  }
  private StatusEnum status = null;

  /**
   **/
  
  @Schema(example = "APL-X5JH-TJKJ-DVGC-5T2V8", required = true, description = "")
  @JsonProperty("account")
  @NotNull
  public String getAccount() {
    return account;
  }
  public void setAccount(String account) {
    this.account = account;
  }

  /**
   * The account public key in a hex string format
   **/
  
  @Schema(example = "39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152", required = true, description = "The account public key in a hex string format")
  @JsonProperty("publicKey")
  @NotNull
  public String getPublicKey() {
    return publicKey;
  }
  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  /**
   **/
  
  @Schema(example = "APL-AHYW-P3YX-V426-4UMP2", description = "")
  @JsonProperty("parent")
  public String getParent() {
    return parent;
  }
  public void setParent(String parent) {
    this.parent = parent;
  }

  /**
   **/
  
  @Schema(example = "45225600000000", description = "")
  @JsonProperty("balance")
  public String getBalance() {
    return balance;
  }
  public void setBalance(String balance) {
    this.balance = balance;
  }

  /**
   **/
  
  @Schema(example = "45225600000000", description = "")
  @JsonProperty("unconfirmedbalance")
  public String getUnconfirmedbalance() {
    return unconfirmedbalance;
  }
  public void setUnconfirmedbalance(String unconfirmedbalance) {
    this.unconfirmedbalance = unconfirmedbalance;
  }

  /**
   **/
  
  @Schema(example = "created", description = "")
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }
  public void setStatus(StatusEnum status) {
    this.status = status;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccountInfo accountInfo = (AccountInfo) o;
    return Objects.equals(account, accountInfo.account) &&
        Objects.equals(publicKey, accountInfo.publicKey) &&
        Objects.equals(parent, accountInfo.parent) &&
        Objects.equals(balance, accountInfo.balance) &&
        Objects.equals(unconfirmedbalance, accountInfo.unconfirmedbalance) &&
        Objects.equals(status, accountInfo.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(account, publicKey, parent, balance, unconfirmedbalance, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccountInfo {\n");
    
    sb.append("    account: ").append(toIndentedString(account)).append("\n");
    sb.append("    publicKey: ").append(toIndentedString(publicKey)).append("\n");
    sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
    sb.append("    balance: ").append(toIndentedString(balance)).append("\n");
    sb.append("    unconfirmedbalance: ").append(toIndentedString(unconfirmedbalance)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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
