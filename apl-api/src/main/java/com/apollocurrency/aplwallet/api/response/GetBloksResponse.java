package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


import java.util.ArrayList;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetBloksResponse extends ResponseBase{
    public List<BlockDTO> blocks = new ArrayList<>();
}
