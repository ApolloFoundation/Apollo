package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class HealthResponse extends BaseResponse  {
  private Integer blockchainHeight = null;  private Integer dbConnectionTotal = null;  private Integer dbConnectionActive = null;  private Integer dbConnectionIdle = null;  private Integer unconfirmedTxCacheSize = null;  private Integer maxUnconfirmedTxCount = null;  private Boolean isTrimActive = null;

  /**
   * Current height of blockchain
   **/
  
  @Schema(example = "9900345", description = "Current height of blockchain")
  @JsonProperty("blockchainHeight")
  public Integer getBlockchainHeight() {
    return blockchainHeight;
  }
  public void setBlockchainHeight(Integer blockchainHeight) {
    this.blockchainHeight = blockchainHeight;
  }

  /**
   * Total connections in the pool
   **/
  
  @Schema(description = "Total connections in the pool")
  @JsonProperty("dbConnectionTotal")
  public Integer getDbConnectionTotal() {
    return dbConnectionTotal;
  }
  public void setDbConnectionTotal(Integer dbConnectionTotal) {
    this.dbConnectionTotal = dbConnectionTotal;
  }

  /**
   * Active connections in the pool
   **/
  
  @Schema(description = "Active connections in the pool")
  @JsonProperty("dbConnectionActive")
  public Integer getDbConnectionActive() {
    return dbConnectionActive;
  }
  public void setDbConnectionActive(Integer dbConnectionActive) {
    this.dbConnectionActive = dbConnectionActive;
  }

  /**
   * Idle connections in the pool
   **/
  
  @Schema(description = "Idle connections in the pool")
  @JsonProperty("dbConnectionIdle")
  public Integer getDbConnectionIdle() {
    return dbConnectionIdle;
  }
  public void setDbConnectionIdle(Integer dbConnectionIdle) {
    this.dbConnectionIdle = dbConnectionIdle;
  }

  /**
   * The waiting transactions cache size
   **/
  
  @Schema(description = "The waiting transactions cache size")
  @JsonProperty("unconfirmedTxCacheSize")
  public Integer getUnconfirmedTxCacheSize() {
    return unconfirmedTxCacheSize;
  }
  public void setUnconfirmedTxCacheSize(Integer unconfirmedTxCacheSize) {
    this.unconfirmedTxCacheSize = unconfirmedTxCacheSize;
  }

  /**
   * The max count of the unconfirmed transactions
   **/
  
  @Schema(description = "The max count of the unconfirmed transactions")
  @JsonProperty("maxUnconfirmedTxCount")
  public Integer getMaxUnconfirmedTxCount() {
    return maxUnconfirmedTxCount;
  }
  public void setMaxUnconfirmedTxCount(Integer maxUnconfirmedTxCount) {
    this.maxUnconfirmedTxCount = maxUnconfirmedTxCount;
  }

  /**
   * Returns true if the trimming process is active
   **/
  
  @Schema(description = "Returns true if the trimming process is active")
  @JsonProperty("isTrimActive")
  public Boolean isIsTrimActive() {
    return isTrimActive;
  }
  public void setIsTrimActive(Boolean isTrimActive) {
    this.isTrimActive = isTrimActive;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HealthResponse healthResponse = (HealthResponse) o;
    return Objects.equals(blockchainHeight, healthResponse.blockchainHeight) &&
        Objects.equals(dbConnectionTotal, healthResponse.dbConnectionTotal) &&
        Objects.equals(dbConnectionActive, healthResponse.dbConnectionActive) &&
        Objects.equals(dbConnectionIdle, healthResponse.dbConnectionIdle) &&
        Objects.equals(unconfirmedTxCacheSize, healthResponse.unconfirmedTxCacheSize) &&
        Objects.equals(maxUnconfirmedTxCount, healthResponse.maxUnconfirmedTxCount) &&
        Objects.equals(isTrimActive, healthResponse.isTrimActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blockchainHeight, dbConnectionTotal, dbConnectionActive, dbConnectionIdle, unconfirmedTxCacheSize, maxUnconfirmedTxCount, isTrimActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HealthResponse {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    blockchainHeight: ").append(toIndentedString(blockchainHeight)).append("\n");
    sb.append("    dbConnectionTotal: ").append(toIndentedString(dbConnectionTotal)).append("\n");
    sb.append("    dbConnectionActive: ").append(toIndentedString(dbConnectionActive)).append("\n");
    sb.append("    dbConnectionIdle: ").append(toIndentedString(dbConnectionIdle)).append("\n");
    sb.append("    unconfirmedTxCacheSize: ").append(toIndentedString(unconfirmedTxCacheSize)).append("\n");
    sb.append("    maxUnconfirmedTxCount: ").append(toIndentedString(maxUnconfirmedTxCount)).append("\n");
    sb.append("    isTrimActive: ").append(toIndentedString(isTrimActive)).append("\n");
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
