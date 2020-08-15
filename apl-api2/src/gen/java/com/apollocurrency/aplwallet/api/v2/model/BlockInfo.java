package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class BlockInfo extends BaseResponse  {
  private String id = null;  private String blockSignature = null;  private Long height = null;  private String generator = null;  private String generationSignature = null;  private String generatorPublicKey = null;  private Long timestamp = null;  private Integer timeout = null;  private Integer version = null;  private String baseTarget = null;  private String cumulativeDifficulty = null;  private String previousBlock = null;  private String previousBlockHash = null;  private String nextBlock = null;  private String payloadHash = null;  private Integer payloadLength = null;  private Integer numberOfTransactions = null;  private String totalAmountATM = null;  private String totalFeeATM = null;

  /**
   **/
  
  @Schema(example = "1130faaeb604315160401", description = "")
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   **/
  
  @Schema(example = "ff080e64436603df0c3b9a5b792b03a26725a83bbe6aa46eb7eed9ee83707f071b6d529d09be1f2594c6f8545c2772a091896bc808553c1774e39a41248b1e1c", description = "")
  @JsonProperty("blockSignature")
  public String getBlockSignature() {
    return blockSignature;
  }
  public void setBlockSignature(String blockSignature) {
    this.blockSignature = blockSignature;
  }

  /**
   **/
  
  @Schema(example = "1319854", description = "")
  @JsonProperty("height")
  public Long getHeight() {
    return height;
  }
  public void setHeight(Long height) {
    this.height = height;
  }

  /**
   **/
  
  @Schema(example = "APL-FXHG-6KHM-23LE-42ACU", description = "")
  @JsonProperty("generator")
  public String getGenerator() {
    return generator;
  }
  public void setGenerator(String generator) {
    this.generator = generator;
  }

  /**
   **/
  
  @Schema(example = "60e598f6276371119720786b05e507cd628665473b24c8f76de436d99cf113f7", description = "")
  @JsonProperty("generationSignature")
  public String getGenerationSignature() {
    return generationSignature;
  }
  public void setGenerationSignature(String generationSignature) {
    this.generationSignature = generationSignature;
  }

  /**
   **/
  
  @Schema(example = "39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152", description = "")
  @JsonProperty("generatorPublicKey")
  public String getGeneratorPublicKey() {
    return generatorPublicKey;
  }
  public void setGeneratorPublicKey(String generatorPublicKey) {
    this.generatorPublicKey = generatorPublicKey;
  }

  /**
   * Block timestamp, Unix timestamp in milliseconds
   **/
  
  @Schema(example = "1591696372000", description = "Block timestamp, Unix timestamp in milliseconds")
  @JsonProperty("timestamp")
  public Long getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   **/
  
  @Schema(example = "1", description = "")
  @JsonProperty("timeout")
  public Integer getTimeout() {
    return timeout;
  }
  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  /**
   * The block version
   **/
  
  @Schema(example = "6", description = "The block version")
  @JsonProperty("version")
  public Integer getVersion() {
    return version;
  }
  public void setVersion(Integer version) {
    this.version = version;
  }

  /**
   **/
  
  @Schema(example = "7686143350", description = "")
  @JsonProperty("baseTarget")
  public String getBaseTarget() {
    return baseTarget;
  }
  public void setBaseTarget(String baseTarget) {
    this.baseTarget = baseTarget;
  }

  /**
   **/
  
  @Schema(example = "8728234277524822", description = "")
  @JsonProperty("cumulativeDifficulty")
  public String getCumulativeDifficulty() {
    return cumulativeDifficulty;
  }
  public void setCumulativeDifficulty(String cumulativeDifficulty) {
    this.cumulativeDifficulty = cumulativeDifficulty;
  }

  /**
   * Prev block id
   **/
  
  @Schema(example = "80faaeb4787337264514", description = "Prev block id")
  @JsonProperty("previousBlock")
  public String getPreviousBlock() {
    return previousBlock;
  }
  public void setPreviousBlock(String previousBlock) {
    this.previousBlock = previousBlock;
  }

  /**
   **/
  
  @Schema(example = "5b8ba14eaebba8cdc682c946947f5352a596d00ac63c4e616785d00cf8b8e016", description = "")
  @JsonProperty("previousBlockHash")
  public String getPreviousBlockHash() {
    return previousBlockHash;
  }
  public void setPreviousBlockHash(String previousBlockHash) {
    this.previousBlockHash = previousBlockHash;
  }

  /**
   * Next block id
   **/
  
  @Schema(example = "140faaeb81930093118053", description = "Next block id")
  @JsonProperty("nextBlock")
  public String getNextBlock() {
    return nextBlock;
  }
  public void setNextBlock(String nextBlock) {
    this.nextBlock = nextBlock;
  }

  /**
   * Hash of the paylod (all transactions)
   **/
  
  @Schema(example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", description = "Hash of the paylod (all transactions)")
  @JsonProperty("payloadHash")
  public String getPayloadHash() {
    return payloadHash;
  }
  public void setPayloadHash(String payloadHash) {
    this.payloadHash = payloadHash;
  }

  /**
   * The length in bytes of all transactions
   **/
  
  @Schema(example = "523423", description = "The length in bytes of all transactions")
  @JsonProperty("payloadLength")
  public Integer getPayloadLength() {
    return payloadLength;
  }
  public void setPayloadLength(Integer payloadLength) {
    this.payloadLength = payloadLength;
  }

  /**
   **/
  
  @Schema(example = "14", description = "")
  @JsonProperty("numberOfTransactions")
  public Integer getNumberOfTransactions() {
    return numberOfTransactions;
  }
  public void setNumberOfTransactions(Integer numberOfTransactions) {
    this.numberOfTransactions = numberOfTransactions;
  }

  /**
   **/
  
  @Schema(example = "569000000000000", description = "")
  @JsonProperty("totalAmountATM")
  public String getTotalAmountATM() {
    return totalAmountATM;
  }
  public void setTotalAmountATM(String totalAmountATM) {
    this.totalAmountATM = totalAmountATM;
  }

  /**
   **/
  
  @Schema(example = "2800000000", description = "")
  @JsonProperty("totalFeeATM")
  public String getTotalFeeATM() {
    return totalFeeATM;
  }
  public void setTotalFeeATM(String totalFeeATM) {
    this.totalFeeATM = totalFeeATM;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlockInfo blockInfo = (BlockInfo) o;
    return Objects.equals(id, blockInfo.id) &&
        Objects.equals(blockSignature, blockInfo.blockSignature) &&
        Objects.equals(height, blockInfo.height) &&
        Objects.equals(generator, blockInfo.generator) &&
        Objects.equals(generationSignature, blockInfo.generationSignature) &&
        Objects.equals(generatorPublicKey, blockInfo.generatorPublicKey) &&
        Objects.equals(timestamp, blockInfo.timestamp) &&
        Objects.equals(timeout, blockInfo.timeout) &&
        Objects.equals(version, blockInfo.version) &&
        Objects.equals(baseTarget, blockInfo.baseTarget) &&
        Objects.equals(cumulativeDifficulty, blockInfo.cumulativeDifficulty) &&
        Objects.equals(previousBlock, blockInfo.previousBlock) &&
        Objects.equals(previousBlockHash, blockInfo.previousBlockHash) &&
        Objects.equals(nextBlock, blockInfo.nextBlock) &&
        Objects.equals(payloadHash, blockInfo.payloadHash) &&
        Objects.equals(payloadLength, blockInfo.payloadLength) &&
        Objects.equals(numberOfTransactions, blockInfo.numberOfTransactions) &&
        Objects.equals(totalAmountATM, blockInfo.totalAmountATM) &&
        Objects.equals(totalFeeATM, blockInfo.totalFeeATM);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, blockSignature, height, generator, generationSignature, generatorPublicKey, timestamp, timeout, version, baseTarget, cumulativeDifficulty, previousBlock, previousBlockHash, nextBlock, payloadHash, payloadLength, numberOfTransactions, totalAmountATM, totalFeeATM);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BlockInfo {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    blockSignature: ").append(toIndentedString(blockSignature)).append("\n");
    sb.append("    height: ").append(toIndentedString(height)).append("\n");
    sb.append("    generator: ").append(toIndentedString(generator)).append("\n");
    sb.append("    generationSignature: ").append(toIndentedString(generationSignature)).append("\n");
    sb.append("    generatorPublicKey: ").append(toIndentedString(generatorPublicKey)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    timeout: ").append(toIndentedString(timeout)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    baseTarget: ").append(toIndentedString(baseTarget)).append("\n");
    sb.append("    cumulativeDifficulty: ").append(toIndentedString(cumulativeDifficulty)).append("\n");
    sb.append("    previousBlock: ").append(toIndentedString(previousBlock)).append("\n");
    sb.append("    previousBlockHash: ").append(toIndentedString(previousBlockHash)).append("\n");
    sb.append("    nextBlock: ").append(toIndentedString(nextBlock)).append("\n");
    sb.append("    payloadHash: ").append(toIndentedString(payloadHash)).append("\n");
    sb.append("    payloadLength: ").append(toIndentedString(payloadLength)).append("\n");
    sb.append("    numberOfTransactions: ").append(toIndentedString(numberOfTransactions)).append("\n");
    sb.append("    totalAmountATM: ").append(toIndentedString(totalAmountATM)).append("\n");
    sb.append("    totalFeeATM: ").append(toIndentedString(totalFeeATM)).append("\n");
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
