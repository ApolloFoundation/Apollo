package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class BlockchainInfo extends BaseResponse  {
  private String chainid = null;  private Long height = null;  private String genesisAccount = null;  private String genesisBlockId = null;  private Long genesisBlockTimestamp = null;  private String ecBlockId = null;  private Long ecBlockHeight = null;  private Long txTimestamp = null;  private Long timestamp = null;

  /**
   **/
  
  @Schema(example = "a2e9b946290b48b69985dc2e5a5860a1", description = "")
  @JsonProperty("chainid")
  public String getChainid() {
    return chainid;
  }
  public void setChainid(String chainid) {
    this.chainid = chainid;
  }

  /**
   **/
  
  @Schema(example = "4789567", description = "")
  @JsonProperty("height")
  public Long getHeight() {
    return height;
  }
  public void setHeight(Long height) {
    this.height = height;
  }

  /**
   **/
  
  @Schema(example = "90001259ec21d31a30898d7", description = "")
  @JsonProperty("genesisAccount")
  public String getGenesisAccount() {
    return genesisAccount;
  }
  public void setGenesisAccount(String genesisAccount) {
    this.genesisAccount = genesisAccount;
  }

  /**
   * Genesis block ID
   **/
  
  @Schema(example = "15856251679437054149169000", description = "Genesis block ID")
  @JsonProperty("genesisBlockId")
  public String getGenesisBlockId() {
    return genesisBlockId;
  }
  public void setGenesisBlockId(String genesisBlockId) {
    this.genesisBlockId = genesisBlockId;
  }

  /**
   * Genesis block timestamp (epoch beginning), Unix timestamp in milliseconds
   **/
  
  @Schema(example = "1491696372000", description = "Genesis block timestamp (epoch beginning), Unix timestamp in milliseconds")
  @JsonProperty("genesisBlockTimestamp")
  public Long getGenesisBlockTimestamp() {
    return genesisBlockTimestamp;
  }
  public void setGenesisBlockTimestamp(Long genesisBlockTimestamp) {
    this.genesisBlockTimestamp = genesisBlockTimestamp;
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
   * Request timestamp in seconds since the genesis block
   **/
  
  @Schema(example = "1591696371", description = "Request timestamp in seconds since the genesis block")
  @JsonProperty("txTimestamp")
  public Long getTxTimestamp() {
    return txTimestamp;
  }
  public void setTxTimestamp(Long txTimestamp) {
    this.txTimestamp = txTimestamp;
  }

  /**
   * Request timestamp, Unix timestamp in milliseconds
   **/
  
  @Schema(example = "1591696372300", description = "Request timestamp, Unix timestamp in milliseconds")
  @JsonProperty("timestamp")
  public Long getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlockchainInfo blockchainInfo = (BlockchainInfo) o;
    return Objects.equals(chainid, blockchainInfo.chainid) &&
        Objects.equals(height, blockchainInfo.height) &&
        Objects.equals(genesisAccount, blockchainInfo.genesisAccount) &&
        Objects.equals(genesisBlockId, blockchainInfo.genesisBlockId) &&
        Objects.equals(genesisBlockTimestamp, blockchainInfo.genesisBlockTimestamp) &&
        Objects.equals(ecBlockId, blockchainInfo.ecBlockId) &&
        Objects.equals(ecBlockHeight, blockchainInfo.ecBlockHeight) &&
        Objects.equals(txTimestamp, blockchainInfo.txTimestamp) &&
        Objects.equals(timestamp, blockchainInfo.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chainid, height, genesisAccount, genesisBlockId, genesisBlockTimestamp, ecBlockId, ecBlockHeight, txTimestamp, timestamp);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BlockchainInfo {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    chainid: ").append(toIndentedString(chainid)).append("\n");
    sb.append("    height: ").append(toIndentedString(height)).append("\n");
    sb.append("    genesisAccount: ").append(toIndentedString(genesisAccount)).append("\n");
    sb.append("    genesisBlockId: ").append(toIndentedString(genesisBlockId)).append("\n");
    sb.append("    genesisBlockTimestamp: ").append(toIndentedString(genesisBlockTimestamp)).append("\n");
    sb.append("    ecBlockId: ").append(toIndentedString(ecBlockId)).append("\n");
    sb.append("    ecBlockHeight: ").append(toIndentedString(ecBlockHeight)).append("\n");
    sb.append("    txTimestamp: ").append(toIndentedString(txTimestamp)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
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
