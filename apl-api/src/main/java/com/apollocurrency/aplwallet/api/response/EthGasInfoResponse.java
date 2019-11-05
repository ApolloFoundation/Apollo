package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class EthGasInfoResponse extends EthGasInfoDto {
    private String fast;
    private String average;
    private String safeLow;

}
