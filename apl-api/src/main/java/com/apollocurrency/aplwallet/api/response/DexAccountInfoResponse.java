package com.apollocurrency.aplwallet.api.response;
import com.apollocurrency.aplwallet.api.dto.DexCurrencyDTO;
import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
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
public class DexAccountInfoResponse extends ResponseBase{
    private List<DexCurrencyDTO> currencies;
}
