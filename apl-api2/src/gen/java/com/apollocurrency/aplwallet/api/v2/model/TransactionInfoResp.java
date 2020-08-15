package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class TransactionInfoResp extends BaseResponse  {
  private String id = null;  private String type = null;  private String subtype = null;  private Boolean phased = null;  private Long timestamp = null;  private Integer deadline = null;  private String senderPublicKey = null;  private String recipient = null;  private String amount = null;  private String fee = null;  private String referencedTransactionFullHash = null;  private String signature = null;  private String signatureHash = null;  private String fullHash = null;  private java.util.Map<String, Object> attachment = new java.util.HashMap<String, Object>();  private String sender = null;  private Long height = null;  private String version = null;  private String ecBlockId = null;  private Long ecBlockHeight = null;  private String block = null;  private Integer confirmations = null;  private Long blockTimestamp = null;  private Integer index = null;

  /**
   * The transaction ID
   **/
  
  @Schema(example = "1a0feb1306043151604016701", description = "The transaction ID")
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Transaction type
   **/
  
  @Schema(example = "2", description = "Transaction type")
  @JsonProperty("type")
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Transaction subtype
   **/
  
  @Schema(example = "1", description = "Transaction subtype")
  @JsonProperty("subtype")
  public String getSubtype() {
    return subtype;
  }
  public void setSubtype(String subtype) {
    this.subtype = subtype;
  }

  /**
   * is true if the transaction is phased
   **/
  
  @Schema(example = "false", description = "is true if the transaction is phased")
  @JsonProperty("phased")
  public Boolean isPhased() {
    return phased;
  }
  public void setPhased(Boolean phased) {
    this.phased = phased;
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
   * the deadline for the transaction to be confirmed in minutes,ex.1440&#x3D;24h
   **/
  
  @Schema(example = "1440", description = "the deadline for the transaction to be confirmed in minutes,ex.1440=24h")
  @JsonProperty("deadline")
  public Integer getDeadline() {
    return deadline;
  }
  public void setDeadline(Integer deadline) {
    this.deadline = deadline;
  }

  /**
   * The public key of the sending account for the transaction
   **/
  
  @Schema(description = "The public key of the sending account for the transaction")
  @JsonProperty("senderPublicKey")
  public String getSenderPublicKey() {
    return senderPublicKey;
  }
  public void setSenderPublicKey(String senderPublicKey) {
    this.senderPublicKey = senderPublicKey;
  }

  /**
   * The account Id of the recipient
   **/
  
  @Schema(example = "APL-FXHG-6KHM-23LE-42ACU", description = "The account Id of the recipient")
  @JsonProperty("recipient")
  public String getRecipient() {
    return recipient;
  }
  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  /**
   * The amount of the transaction
   **/
  
  @Schema(example = "34500000000", description = "The amount of the transaction")
  @JsonProperty("amount")
  public String getAmount() {
    return amount;
  }
  public void setAmount(String amount) {
    this.amount = amount;
  }

  /**
   * The fee of the transaction
   **/
  
  @Schema(example = "200000000", description = "The fee of the transaction")
  @JsonProperty("fee")
  public String getFee() {
    return fee;
  }
  public void setFee(String fee) {
    this.fee = fee;
  }

  /**
   * The full hash of a transaction referenced by this one
   **/
  
  @Schema(example = "5424ca4dd976a873839c7d5c9952fa15ae619f678365e8ebbc73f967142eb40d", description = "The full hash of a transaction referenced by this one")
  @JsonProperty("referencedTransactionFullHash")
  public String getReferencedTransactionFullHash() {
    return referencedTransactionFullHash;
  }
  public void setReferencedTransactionFullHash(String referencedTransactionFullHash) {
    this.referencedTransactionFullHash = referencedTransactionFullHash;
  }

  /**
   * The digital signature of the transaction
   **/
  
  @Schema(example = "920173ae606d36c1c77fc5bdf294bd048d04f85c46535525771b524dbb1ed20b73311900d4a409c293a10d8a5ab987430be4bd7478fb16a41cf775afa33c4d56", description = "The digital signature of the transaction")
  @JsonProperty("signature")
  public String getSignature() {
    return signature;
  }
  public void setSignature(String signature) {
    this.signature = signature;
  }

  /**
   * SHA-256 hash of the transaction signature
   **/
  
  @Schema(example = "2fc1883e9b76fdc67fabee0a3def5f4b7fc2de9cd65bc8bb6d39eaf5e99498d8", description = "SHA-256 hash of the transaction signature")
  @JsonProperty("signatureHash")
  public String getSignatureHash() {
    return signatureHash;
  }
  public void setSignatureHash(String signatureHash) {
    this.signatureHash = signatureHash;
  }

  /**
   * The full hash of the transaction
   **/
  
  @Schema(example = "39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152", description = "The full hash of the transaction")
  @JsonProperty("fullHash")
  public String getFullHash() {
    return fullHash;
  }
  public void setFullHash(String fullHash) {
    this.fullHash = fullHash;
  }

  /**
   * An object containong any additional data needed for the transaction
   **/
  
  @Schema(description = "An object containong any additional data needed for the transaction")
  @JsonProperty("attachment")
  public java.util.Map<String, Object> getAttachment() {
    return attachment;
  }
  public void setAttachment(java.util.Map<String, Object> attachment) {
    this.attachment = attachment;
  }

  /**
   * The sender account
   **/
  
  @Schema(example = "APL-X5JH-TJKJ-DVGC-5T2V8", description = "The sender account")
  @JsonProperty("sender")
  public String getSender() {
    return sender;
  }
  public void setSender(String sender) {
    this.sender = sender;
  }

  /**
   * The height of the block in the blockchain
   **/
  
  @Schema(example = "4365987", description = "The height of the block in the blockchain")
  @JsonProperty("height")
  public Long getHeight() {
    return height;
  }
  public void setHeight(Long height) {
    this.height = height;
  }

  /**
   * The transaction version number
   **/
  
  @Schema(example = "1", description = "The transaction version number")
  @JsonProperty("version")
  public String getVersion() {
    return version;
  }
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * The economic clustering block ID
   **/
  
  @Schema(example = "40faaeb15856251679437054", description = "The economic clustering block ID")
  @JsonProperty("ecBlockId")
  public String getEcBlockId() {
    return ecBlockId;
  }
  public void setEcBlockId(String ecBlockId) {
    this.ecBlockId = ecBlockId;
  }

  /**
   * The economic clustering block height
   **/
  
  @Schema(example = "3301233", description = "The economic clustering block height")
  @JsonProperty("ecBlockHeight")
  public Long getEcBlockHeight() {
    return ecBlockHeight;
  }
  public void setEcBlockHeight(Long ecBlockHeight) {
    this.ecBlockHeight = ecBlockHeight;
  }

  /**
   * The block id
   **/
  
  @Schema(example = "230aeb0f4585625167943", description = "The block id")
  @JsonProperty("block")
  public String getBlock() {
    return block;
  }
  public void setBlock(String block) {
    this.block = block;
  }

  /**
   * the number of transaction confirmations
   **/
  
  @Schema(example = "387", description = "the number of transaction confirmations")
  @JsonProperty("confirmations")
  public Integer getConfirmations() {
    return confirmations;
  }
  public void setConfirmations(Integer confirmations) {
    this.confirmations = confirmations;
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionInfoResp transactionInfoResp = (TransactionInfoResp) o;
    return Objects.equals(id, transactionInfoResp.id) &&
        Objects.equals(type, transactionInfoResp.type) &&
        Objects.equals(subtype, transactionInfoResp.subtype) &&
        Objects.equals(phased, transactionInfoResp.phased) &&
        Objects.equals(timestamp, transactionInfoResp.timestamp) &&
        Objects.equals(deadline, transactionInfoResp.deadline) &&
        Objects.equals(senderPublicKey, transactionInfoResp.senderPublicKey) &&
        Objects.equals(recipient, transactionInfoResp.recipient) &&
        Objects.equals(amount, transactionInfoResp.amount) &&
        Objects.equals(fee, transactionInfoResp.fee) &&
        Objects.equals(referencedTransactionFullHash, transactionInfoResp.referencedTransactionFullHash) &&
        Objects.equals(signature, transactionInfoResp.signature) &&
        Objects.equals(signatureHash, transactionInfoResp.signatureHash) &&
        Objects.equals(fullHash, transactionInfoResp.fullHash) &&
        Objects.equals(attachment, transactionInfoResp.attachment) &&
        Objects.equals(sender, transactionInfoResp.sender) &&
        Objects.equals(height, transactionInfoResp.height) &&
        Objects.equals(version, transactionInfoResp.version) &&
        Objects.equals(ecBlockId, transactionInfoResp.ecBlockId) &&
        Objects.equals(ecBlockHeight, transactionInfoResp.ecBlockHeight) &&
        Objects.equals(block, transactionInfoResp.block) &&
        Objects.equals(confirmations, transactionInfoResp.confirmations) &&
        Objects.equals(blockTimestamp, transactionInfoResp.blockTimestamp) &&
        Objects.equals(index, transactionInfoResp.index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, subtype, phased, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransactionFullHash, signature, signatureHash, fullHash, attachment, sender, height, version, ecBlockId, ecBlockHeight, block, confirmations, blockTimestamp, index);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TransactionInfoResp {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    subtype: ").append(toIndentedString(subtype)).append("\n");
    sb.append("    phased: ").append(toIndentedString(phased)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    deadline: ").append(toIndentedString(deadline)).append("\n");
    sb.append("    senderPublicKey: ").append(toIndentedString(senderPublicKey)).append("\n");
    sb.append("    recipient: ").append(toIndentedString(recipient)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    fee: ").append(toIndentedString(fee)).append("\n");
    sb.append("    referencedTransactionFullHash: ").append(toIndentedString(referencedTransactionFullHash)).append("\n");
    sb.append("    signature: ").append(toIndentedString(signature)).append("\n");
    sb.append("    signatureHash: ").append(toIndentedString(signatureHash)).append("\n");
    sb.append("    fullHash: ").append(toIndentedString(fullHash)).append("\n");
    sb.append("    attachment: ").append(toIndentedString(attachment)).append("\n");
    sb.append("    sender: ").append(toIndentedString(sender)).append("\n");
    sb.append("    height: ").append(toIndentedString(height)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    ecBlockId: ").append(toIndentedString(ecBlockId)).append("\n");
    sb.append("    ecBlockHeight: ").append(toIndentedString(ecBlockHeight)).append("\n");
    sb.append("    block: ").append(toIndentedString(block)).append("\n");
    sb.append("    confirmations: ").append(toIndentedString(confirmations)).append("\n");
    sb.append("    blockTimestamp: ").append(toIndentedString(blockTimestamp)).append("\n");
    sb.append("    index: ").append(toIndentedString(index)).append("\n");
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
