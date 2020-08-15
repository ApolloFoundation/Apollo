package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class QueryResult extends BaseResponse  {
  private Integer count = null;  private java.util.List<TxReceipt> result = new java.util.ArrayList<TxReceipt>();  private QueryObject query = null;

  /**
   * Count of result items
   **/
  
  @Schema(example = "345", description = "Count of result items")
  @JsonProperty("count")
  public Integer getCount() {
    return count;
  }
  public void setCount(Integer count) {
    this.count = count;
  }

  /**
   **/
  
  @Schema(description = "")
  @JsonProperty("result")
  public java.util.List<TxReceipt> getResult() {
    return result;
  }
  public void setResult(java.util.List<TxReceipt> result) {
    this.result = result;
  }

  /**
   **/
  
  @Schema(description = "")
  @JsonProperty("query")
  public QueryObject getQuery() {
    return query;
  }
  public void setQuery(QueryObject query) {
    this.query = query;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QueryResult queryResult = (QueryResult) o;
    return Objects.equals(count, queryResult.count) &&
        Objects.equals(result, queryResult.result) &&
        Objects.equals(query, queryResult.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, result, query);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QueryResult {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
    sb.append("    result: ").append(toIndentedString(result)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
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
