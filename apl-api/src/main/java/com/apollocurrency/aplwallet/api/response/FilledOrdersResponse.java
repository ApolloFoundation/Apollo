package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.DepositsInfoDTO;
import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FilledOrdersResponse extends ResponseBase{
    private String address;
    private List<DepositsInfoDTO> depositsInfo;
}
