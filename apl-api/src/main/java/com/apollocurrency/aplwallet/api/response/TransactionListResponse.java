package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class TransactionListResponse {
    private List<TransactionDTO> transactions;
    private List<TransactionDTO> unconfirmedTransactions;

    @Override
    public String toString() {
        return "TransactionListResponse{" +
                "transactions=[" + (transactions != null ? transactions.size() : -1 ) +
                "]}";
    }
}
