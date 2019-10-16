package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.AssetTradeDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class AssetTradeResponse {
    private List<AssetTradeDTO> trades;
}
