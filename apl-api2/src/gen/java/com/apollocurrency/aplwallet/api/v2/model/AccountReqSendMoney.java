package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class AccountReqSendMoney   {
  private String parent = null;  private String psecret = null;  private String sender = null;  private String csecret = null;  private String recipient = null;  private Long amount = null;

  /**
   * parent account id
   **/
  
  @Schema(example = "APL-X5JH-TJKJ-DVGC-5T2V8", required = true, description = "parent account id")
  @JsonProperty("parent")
  @NotNull
  public String getParent() {
    return parent;
  }
  public void setParent(String parent) {
    this.parent = parent;
  }

  /**
   * parent account secret phrase
   **/
  
  @Schema(example = "ParenttopSecretPhrase", description = "parent account secret phrase")
  @JsonProperty("psecret")
  public String getPsecret() {
    return psecret;
  }
  public void setPsecret(String psecret) {
    this.psecret = psecret;
  }

  /**
   * child account id
   **/
  
  @Schema(example = "APL-632K-TWX3-2ALQ-973CU", description = "child account id")
  @JsonProperty("sender")
  public String getSender() {
    return sender;
  }
  public void setSender(String sender) {
    this.sender = sender;
  }

  /**
   * child account secret phrase
   **/
  
  @Schema(example = "ChildtopSecretPhrase", description = "child account secret phrase")
  @JsonProperty("csecret")
  public String getCsecret() {
    return csecret;
  }
  public void setCsecret(String csecret) {
    this.csecret = csecret;
  }

  /**
   * recipient account id
   **/
  
  @Schema(example = "APL-VWMY-APVK-UFHN-3MC7N", description = "recipient account id")
  @JsonProperty("recipient")
  public String getRecipient() {
    return recipient;
  }
  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  /**
   * the amount
   **/
  
  @Schema(description = "the amount")
  @JsonProperty("amount")
  public Long getAmount() {
    return amount;
  }
  public void setAmount(Long amount) {
    this.amount = amount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccountReqSendMoney accountReqSendMoney = (AccountReqSendMoney) o;
    return Objects.equals(parent, accountReqSendMoney.parent) &&
        Objects.equals(psecret, accountReqSendMoney.psecret) &&
        Objects.equals(sender, accountReqSendMoney.sender) &&
        Objects.equals(csecret, accountReqSendMoney.csecret) &&
        Objects.equals(recipient, accountReqSendMoney.recipient) &&
        Objects.equals(amount, accountReqSendMoney.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent, psecret, sender, csecret, recipient, amount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccountReqSendMoney {\n");
    
    sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
    sb.append("    psecret: ").append(toIndentedString(psecret)).append("\n");
    sb.append("    sender: ").append(toIndentedString(sender)).append("\n");
    sb.append("    csecret: ").append(toIndentedString(csecret)).append("\n");
    sb.append("    recipient: ").append(toIndentedString(recipient)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
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
