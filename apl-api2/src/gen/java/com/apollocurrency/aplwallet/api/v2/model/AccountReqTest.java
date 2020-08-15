package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class AccountReqTest   {
  private String parent = null;  private String secret = null;  private java.util.List<String> childSecretList = new java.util.ArrayList<String>();

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
   * parent account secret phrase
   **/
  
  @Schema(example = "topSecretPhrase", description = "parent account secret phrase")
  @JsonProperty("secret")
  public String getSecret() {
    return secret;
  }
  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * list of child secret phrases
   **/
  
  @Schema(description = "list of child secret phrases")
  @JsonProperty("child_secret_list")
 @Size(max=128)  public java.util.List<String> getChildSecretList() {
    return childSecretList;
  }
  public void setChildSecretList(java.util.List<String> childSecretList) {
    this.childSecretList = childSecretList;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccountReqTest accountReqTest = (AccountReqTest) o;
    return Objects.equals(parent, accountReqTest.parent) &&
        Objects.equals(secret, accountReqTest.secret) &&
        Objects.equals(childSecretList, accountReqTest.childSecretList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent, secret, childSecretList);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccountReqTest {\n");
    
    sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
    sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
    sb.append("    childSecretList: ").append(toIndentedString(childSecretList)).append("\n");
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
