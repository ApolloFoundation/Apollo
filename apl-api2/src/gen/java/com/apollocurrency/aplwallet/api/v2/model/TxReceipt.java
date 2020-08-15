package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description="Transaction receipt")
public class TxReceipt extends BaseResponse  {
  private String transaction = null;  private String sender = null;  private String recipient = null;  private String signature = null;  private Long timestamp = null;  private String amount = null;  private String fee = null;  private String payload = null;  /**
   * The transaction status. A transaction is considered as unconfirmed until it is included in a valid block. A transaction is considered as confirmed after at least one confirmations, so a transaction is considered as irreversible after 721 confirmations and finally a transaction is permanent if it&#x27;s confirmed 1440 times. 
   */
  public enum StatusEnum {
    UNCONFIRMED("unconfirmed"),
    CONFIRMED("confirmed"),
    IRREVERSIBLE("irreversible"),
    PERMANENT("permanent");
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
  private StatusEnum status = null;  private String block = null;  private Long blockTimestamp = null;  private Long height = null;  private Integer index = null;  private Long confirmations = null;

  /**
   **/
  
  @Schema(example = "8330faaeb404178613417", required = true, description = "")
  @JsonProperty("transaction")
  @NotNull
  public String getTransaction() {
    return transaction;
  }
  public void setTransaction(String transaction) {
    this.transaction = transaction;
  }

  /**
   **/
  
  @Schema(example = "APL-X5JH-TJKJ-DVGC-5T2V8", required = true, description = "")
  @JsonProperty("sender")
  @NotNull
  public String getSender() {
    return sender;
  }
  public void setSender(String sender) {
    this.sender = sender;
  }

  /**
   **/
  
  @Schema(example = "APL-FXHG-6KHM-23LE-42ACU", required = true, description = "")
  @JsonProperty("recipient")
  @NotNull
  public String getRecipient() {
    return recipient;
  }
  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  /**
   **/
  
  @Schema(example = "B98CB1890E76C772A10994B210ED9CF7F9A5488672A5D82C2734BBF9D11505D1", required = true, description = "")
  @JsonProperty("signature")
  @NotNull
  public String getSignature() {
    return signature;
  }
  public void setSignature(String signature) {
    this.signature = signature;
  }

  /**
   * Transaction timestamp, Unix timestamp in milliseconds
   **/
  
  @Schema(example = "1591696372000", description = "Transaction timestamp, Unix timestamp in milliseconds")
  @JsonProperty("timestamp")
  public Long getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   **/
  
  @Schema(example = "34500000000", description = "")
  @JsonProperty("amount")
  public String getAmount() {
    return amount;
  }
  public void setAmount(String amount) {
    this.amount = amount;
  }

  /**
   **/
  
  @Schema(example = "0", description = "")
  @JsonProperty("fee")
  public String getFee() {
    return fee;
  }
  public void setFee(String fee) {
    this.fee = fee;
  }

  /**
   **/
  
  @Schema(example = "{\"transaction\":\"1234567890\", \"amount\":\"1234567890.12\"}", description = "")
  @JsonProperty("payload")
  public String getPayload() {
    return payload;
  }
  public void setPayload(String payload) {
    this.payload = payload;
  }

  /**
   * The transaction status. A transaction is considered as unconfirmed until it is included in a valid block. A transaction is considered as confirmed after at least one confirmations, so a transaction is considered as irreversible after 721 confirmations and finally a transaction is permanent if it&#x27;s confirmed 1440 times. 
   **/
  
  @Schema(example = "unconfirmed", description = "The transaction status. A transaction is considered as unconfirmed until it is included in a valid block. A transaction is considered as confirmed after at least one confirmations, so a transaction is considered as irreversible after 721 confirmations and finally a transaction is permanent if it's confirmed 1440 times. ")
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  /**
   * The Id of the block containing the confirmed transaction
   **/
  
  @Schema(example = "40faaeb1585625167943", description = "The Id of the block containing the confirmed transaction")
  @JsonProperty("block")
  public String getBlock() {
    return block;
  }
  public void setBlock(String block) {
    this.block = block;
  }

  /**
   * Block timestamp, Unix timestamp in milliseconds
   **/
  
  @Schema(example = "1591696372000", description = "Block timestamp, Unix timestamp in milliseconds")
  @JsonProperty("blockTimestamp")
  public Long getBlockTimestamp() {
    return blockTimestamp;
  }
  public void setBlockTimestamp(Long blockTimestamp) {
    this.blockTimestamp = blockTimestamp;
  }

  /**
   **/
  
  @Schema(example = "15623658", description = "")
  @JsonProperty("height")
  public Long getHeight() {
    return height;
  }
  public void setHeight(Long height) {
    this.height = height;
  }

  /**
   * the order of the transaction in the block
   **/
  
  @Schema(example = "1", description = "the order of the transaction in the block")
  @JsonProperty("index")
  public Integer getIndex() {
    return index;
  }
  public void setIndex(Integer index) {
    this.index = index;
  }

  /**
   * the amount of block transaction
   **/
  
  @Schema(example = "1758", description = "the amount of block transaction")
  @JsonProperty("confirmations")
  public Long getConfirmations() {
    return confirmations;
  }
  public void setConfirmations(Long confirmations) {
    this.confirmations = confirmations;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TxReceipt txReceipt = (TxReceipt) o;
    return Objects.equals(transaction, txReceipt.transaction) &&
        Objects.equals(sender, txReceipt.sender) &&
        Objects.equals(recipient, txReceipt.recipient) &&
        Objects.equals(signature, txReceipt.signature) &&
        Objects.equals(timestamp, txReceipt.timestamp) &&
        Objects.equals(amount, txReceipt.amount) &&
        Objects.equals(fee, txReceipt.fee) &&
        Objects.equals(payload, txReceipt.payload) &&
        Objects.equals(status, txReceipt.status) &&
        Objects.equals(block, txReceipt.block) &&
        Objects.equals(blockTimestamp, txReceipt.blockTimestamp) &&
        Objects.equals(height, txReceipt.height) &&
        Objects.equals(index, txReceipt.index) &&
        Objects.equals(confirmations, txReceipt.confirmations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transaction, sender, recipient, signature, timestamp, amount, fee, payload, status, block, blockTimestamp, height, index, confirmations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TxReceipt {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    transaction: ").append(toIndentedString(transaction)).append("\n");
    sb.append("    sender: ").append(toIndentedString(sender)).append("\n");
    sb.append("    recipient: ").append(toIndentedString(recipient)).append("\n");
    sb.append("    signature: ").append(toIndentedString(signature)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    fee: ").append(toIndentedString(fee)).append("\n");
    sb.append("    payload: ").append(toIndentedString(payload)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    block: ").append(toIndentedString(block)).append("\n");
    sb.append("    blockTimestamp: ").append(toIndentedString(blockTimestamp)).append("\n");
    sb.append("    height: ").append(toIndentedString(height)).append("\n");
    sb.append("    index: ").append(toIndentedString(index)).append("\n");
    sb.append("    confirmations: ").append(toIndentedString(confirmations)).append("\n");
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
