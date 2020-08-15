package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class AccountReq   {
  private String parent = null;  private java.util.List<String> childPublicKeyList = new java.util.ArrayList<String>();

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
   * list of child account public keys
   **/
  
  @Schema(description = "list of child account public keys")
  @JsonProperty("child_publicKey_list")
 @Size(max=128)  public java.util.List<String> getChildPublicKeyList() {
    return childPublicKeyList;
  }
  public void setChildPublicKeyList(java.util.List<String> childPublicKeyList) {
    this.childPublicKeyList = childPublicKeyList;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccountReq accountReq = (AccountReq) o;
    return Objects.equals(parent, accountReq.parent) &&
        Objects.equals(childPublicKeyList, accountReq.childPublicKeyList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent, childPublicKeyList);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccountReq {\n");
    
    sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
    sb.append("    childPublicKeyList: ").append(toIndentedString(childPublicKeyList)).append("\n");
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
