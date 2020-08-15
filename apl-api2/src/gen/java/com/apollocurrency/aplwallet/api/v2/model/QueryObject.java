package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class QueryObject   {
  private Integer type = -1;  private java.util.List<String> accounts = new java.util.ArrayList<String>();  private Long first = null;  private Long last = null;  private Long startTime = null;  private Long endTime = null;  private Integer page = 1;  private Integer perPage = 25;  /**
   * Gets or Sets orderBy
   */
  public enum OrderByEnum {
    ASC("asc"),
    DESC("desc");
    private String value;

    OrderByEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }
  }
  private OrderByEnum orderBy = OrderByEnum.ASC;

  /**
   * The transaction type, it&#x27;s an optional parameter and can be missed or specified negative value to avoid filtering by that criteria. There are eleven types:   PAYMENT &#x3D; 0;   MESSAGING &#x3D; 1;   COLORED_COINS &#x3D; 2;   DIGITAL_GOODS &#x3D; 3;   ACCOUNT_CONTROL &#x3D; 4;   MONETARY_SYSTEM &#x3D; 5;   DATA &#x3D; 6;   SHUFFLING &#x3D; 7;   UPDATE &#x3D; 8;   DEX &#x3D; 9;   CHILD_ACCOUNT &#x3D; 10; 
   **/
  
  @Schema(example = "0", description = "The transaction type, it's an optional parameter and can be missed or specified negative value to avoid filtering by that criteria. There are eleven types:   PAYMENT = 0;   MESSAGING = 1;   COLORED_COINS = 2;   DIGITAL_GOODS = 3;   ACCOUNT_CONTROL = 4;   MONETARY_SYSTEM = 5;   DATA = 6;   SHUFFLING = 7;   UPDATE = 8;   DEX = 9;   CHILD_ACCOUNT = 10; ")
  @JsonProperty("type")
  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
  }

  /**
   * Exactly all items.
   **/
  
  @Schema(description = "Exactly all items.")
  @JsonProperty("accounts")
  public java.util.List<String> getAccounts() {
    return accounts;
  }
  public void setAccounts(java.util.List<String> accounts) {
    this.accounts = accounts;
  }

  /**
   * The first block height
   **/
  
  @Schema(example = "4000123", description = "The first block height")
  @JsonProperty("first")
  public Long getFirst() {
    return first;
  }
  public void setFirst(Long first) {
    this.first = first;
  }

  /**
   * The last block height
   **/
  
  @Schema(example = "4999000", description = "The last block height")
  @JsonProperty("last")
  public Long getLast() {
    return last;
  }
  public void setLast(Long last) {
    this.last = last;
  }

  /**
   * The start of the time period, Unix timestamp in milliseconds
   **/
  
  @Schema(example = "1591696310000", description = "The start of the time period, Unix timestamp in milliseconds")
  @JsonProperty("startTime")
  public Long getStartTime() {
    return startTime;
  }
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  /**
   * The end of the time period, Unix timestamp in milliseconds
   **/
  
  @Schema(example = "1591696372000", description = "The end of the time period, Unix timestamp in milliseconds")
  @JsonProperty("endTime")
  public Long getEndTime() {
    return endTime;
  }
  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  /**
   * page number (1-based)
   **/
  
  @Schema(example = "1", description = "page number (1-based)")
  @JsonProperty("page")
  public Integer getPage() {
    return page;
  }
  public void setPage(Integer page) {
    this.page = page;
  }

  /**
   * Number of entries per page (max&#x3D;100)
   **/
  
  @Schema(example = "25", description = "Number of entries per page (max=100)")
  @JsonProperty("perPage")
  public Integer getPerPage() {
    return perPage;
  }
  public void setPerPage(Integer perPage) {
    this.perPage = perPage;
  }

  /**
   **/
  
  @Schema(description = "")
  @JsonProperty("orderBy")
  public OrderByEnum getOrderBy() {
    return orderBy;
  }
  public void setOrderBy(OrderByEnum orderBy) {
    this.orderBy = orderBy;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QueryObject queryObject = (QueryObject) o;
    return Objects.equals(type, queryObject.type) &&
        Objects.equals(accounts, queryObject.accounts) &&
        Objects.equals(first, queryObject.first) &&
        Objects.equals(last, queryObject.last) &&
        Objects.equals(startTime, queryObject.startTime) &&
        Objects.equals(endTime, queryObject.endTime) &&
        Objects.equals(page, queryObject.page) &&
        Objects.equals(perPage, queryObject.perPage) &&
        Objects.equals(orderBy, queryObject.orderBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, accounts, first, last, startTime, endTime, page, perPage, orderBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QueryObject {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    accounts: ").append(toIndentedString(accounts)).append("\n");
    sb.append("    first: ").append(toIndentedString(first)).append("\n");
    sb.append("    last: ").append(toIndentedString(last)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    perPage: ").append(toIndentedString(perPage)).append("\n");
    sb.append("    orderBy: ").append(toIndentedString(orderBy)).append("\n");
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
