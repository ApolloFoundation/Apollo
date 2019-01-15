
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Response with block Id value
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Unique Block Id")
@ApiModel
public class BlockIdResponse extends ResponseBase {
    @ApiModelProperty("Block id as string)")
    public String block;

    @Override
    public String toString() {
        return "BlockIdResponse{" +
                "blockId='" + block + '\'' +
                "}";
    }
}
