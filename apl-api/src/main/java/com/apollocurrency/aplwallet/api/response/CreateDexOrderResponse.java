package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.CreateDexOrderDTO;
import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
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

public class CreateDexOrderResponse extends ResponseBase {
    private String frozenTx;
    private CreateDexOrderDTO order;
    private CreateDexOrderDTO contract;
}
