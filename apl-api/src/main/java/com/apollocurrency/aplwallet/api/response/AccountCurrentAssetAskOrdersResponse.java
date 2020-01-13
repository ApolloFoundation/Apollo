package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.AccountAssetOrderDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class AccountCurrentAssetAskOrdersResponse extends ResponseBase {
    private List<AccountAssetOrderDTO> askOrders;
}
