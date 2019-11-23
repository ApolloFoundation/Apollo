package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTransactionResponse extends ResponseBase {
    private String signatureHash;
    private TransactionDTO transactionJSON;
    private String unsignedTransactionBytes;
    private Boolean broadcasted;
    private String transactionBytes;
    private String fullHash;
    private String transaction;
}
