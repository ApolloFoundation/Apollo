package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.TaggedDataDTO;
import com.fasterxml.jackson.annotation.JsonInclude;


import java.util.List;

//@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListTaggedDataResponse extends ResponseBase {

    //@ApiModelProperty(value = "List of Tagged data entities")
    public List<TaggedDataDTO> data;

    public ListTaggedDataResponse(List<TaggedDataDTO> data) {
        this.data = data;
    }
}
