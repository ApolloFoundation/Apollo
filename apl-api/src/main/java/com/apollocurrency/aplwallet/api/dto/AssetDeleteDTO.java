package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDeleteDTO extends AccountAssetDTO {
    private String assetDelete;
    private Long height;
    private Boolean phased;
}
