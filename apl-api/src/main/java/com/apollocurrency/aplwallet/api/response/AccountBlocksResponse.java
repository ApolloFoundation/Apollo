package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.fasterxml.jackson.annotation.JsonInclude;



import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Blocks of Account representation")
public class AccountBlocksResponse extends ResponseBase{
    //@ApiModelProperty(value = "List of blocks in account", allowEmptyValue = false)
    public List<BlockDTO> blockDTOS = new ArrayList<>();
}
