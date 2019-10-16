package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter @Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDTO extends UnconfirmedTransactionDTO {
    private String block;
    private Integer confirmations;
    private Integer blockTimestamp;
    private Short transactionIndex;

    public TransactionDTO(UnconfirmedTransactionDTO o) {
        super(o);
    }

}
