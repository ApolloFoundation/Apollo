package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.AssetDeleteDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class ExpectedAssetDeletes extends ResponseBase {
    private List<AssetDeleteDTO> deletes;
}
