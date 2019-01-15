package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.TaggedDataDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListTaggedDataResponse extends ResponseBase {

    @ApiModelProperty(value = "List of Tagged data entities")
    public List<TaggedDataDTO> data;

    public ListTaggedDataResponse(List<TaggedDataDTO> data) {
        this.data = data;
    }
}
