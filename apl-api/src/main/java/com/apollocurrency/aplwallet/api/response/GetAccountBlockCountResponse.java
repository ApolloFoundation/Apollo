package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class GetAccountBlockCountResponse extends ResponseBase {
    @ApiModelProperty("Counted number of blocks")
    public Integer numberOfBlocks;
}
