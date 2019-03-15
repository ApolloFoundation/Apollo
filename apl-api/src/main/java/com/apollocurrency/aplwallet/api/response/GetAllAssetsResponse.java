package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.AssetDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAllAssetsResponse extends ResponseBase{
    public AssetDTO[] assets;
}
