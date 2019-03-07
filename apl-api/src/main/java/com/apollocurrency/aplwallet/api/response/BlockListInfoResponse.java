
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Response with list of found blocks
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "List of Block with optional transaction/attachment data included")
//@ApiModel
public class BlockListInfoResponse extends ResponseBase {

    //@ApiModelProperty(value = "Block list", allowEmptyValue = true)
    public List<BlockInfoResponse> blocks = new ArrayList<>(0);

    @Override
    public String toString() {
        return "BlockListInfoResponse{" +
                "blocks=[" + (blocks != null ? blocks.size() : 0) +
                "]}";
    }
}
