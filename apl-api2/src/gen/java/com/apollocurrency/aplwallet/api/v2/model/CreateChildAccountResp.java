package com.apollocurrency.aplwallet.api.v2.model;

import java.util.Objects;
import java.util.ArrayList;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


public class CreateChildAccountResp extends BaseResponse  {
  private String parent = null;  private String tx = null;

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
   * Unsigned transaction data
   **/
  
  @Schema(example = "001047857c04a00539dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152eef5365f2400292d00ca9a3b0000000000c2eb0b000000000000000000000000000000000000000000000000000000000000000000000000920173ae606d36c1c77fc5bdf294bd048d04f85c46535525771b524dbb1ed20b73311900d4a409c293a10d8a5ab987430be4bd7478fb16a41cf775afa33c4d5600000000f1da1300fe7062e6fb2fbb37", required = true, description = "Unsigned transaction data")
  @JsonProperty("tx")
  @NotNull
  public String getTx() {
    return tx;
  }
  public void setTx(String tx) {
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
    CreateChildAccountResp createChildAccountResp = (CreateChildAccountResp) o;
    return Objects.equals(parent, createChildAccountResp.parent) &&
        Objects.equals(tx, createChildAccountResp.tx);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent, tx);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateChildAccountResp {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
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
