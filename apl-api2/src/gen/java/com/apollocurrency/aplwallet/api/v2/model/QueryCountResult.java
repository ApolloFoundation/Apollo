package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class QueryCountResult extends BaseResponse  {
  private Long count = null;  private QueryObject query = null;

  /**
   **/
  
  @Schema(example = "345", description = "")
  @JsonProperty("count")
  public Long getCount() {
    return count;
  }
  public void setCount(Long count) {
    this.count = count;
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
    QueryCountResult queryCountResult = (QueryCountResult) o;
    return Objects.equals(count, queryCountResult.count) &&
        Objects.equals(query, queryCountResult.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, query);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QueryCountResult {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
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
