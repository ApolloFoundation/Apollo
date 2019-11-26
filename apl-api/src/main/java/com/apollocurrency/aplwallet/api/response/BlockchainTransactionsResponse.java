package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockchainTransactionsResponse extends ResponseBase {
    private String serverPublicKey;
    private float requestProcessingTime;
    private List<TransactionDTO> transactions;
    private List<TransactionDTO> unconfirmedTransactionDTOS;
}
