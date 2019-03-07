
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;


/**
 * Response EC block info
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Block's info with three fields")
//@ApiModel
public class BlockEcResponse extends ResponseBase {
   // @ApiModelProperty("EC block id")
    public String ecBlockId;
    //@ApiModelProperty("EC block height")
    public Integer ecBlockHeight;
   // @ApiModelProperty("Block timestamp")
    public String timestamp;

    @Override
    public String toString() {
        return "BlockEcResponse{" +
                "ecBlockId='" + ecBlockId + '\'' +
                ", ecBlockHeight='" + ecBlockHeight + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
