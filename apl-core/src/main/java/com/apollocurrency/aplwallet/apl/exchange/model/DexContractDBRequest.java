package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DexContractDBRequest {
    private Long recipient;
    private Long sender;
    private Integer status;
    private Long offerId;
    private Long counterOfferId;

}
