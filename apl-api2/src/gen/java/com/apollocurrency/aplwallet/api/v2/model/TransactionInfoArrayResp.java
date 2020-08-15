package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class TransactionInfoArrayResp extends BaseResponse  {
  private java.util.List<TransactionInfo> tx = new java.util.ArrayList<TransactionInfo>();

  /**
   * The array of transactions
   **/
  
  @Schema(description = "The array of transactions")
  @JsonProperty("tx")
  public java.util.List<TransactionInfo> getTx() {
    return tx;
  }
  public void setTx(java.util.List<TransactionInfo> tx) {
    this.tx = tx;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionInfoArrayResp transactionInfoArrayResp = (TransactionInfoArrayResp) o;
    return Objects.equals(tx, transactionInfoArrayResp.tx);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tx);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TransactionInfoArrayResp {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    tx: ").append(toIndentedString(tx)).append("\n");
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
