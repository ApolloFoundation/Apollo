package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class GetAccountBlockIdsResponse extends ResponseBase{
    @ApiModelProperty("Block Id list")
    public List<String> blockIds = new ArrayList<>(0);
}
