package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTransactionResponse extends ResponseBase {
    private UnconfirmedTransactionDTO transactionJSON;
    private String signatureHash;
    private String unsignedTransactionBytes;
    private Boolean broadcasted;
    private String transactionBytes;
    private String fullHash;
    private String transaction;
}
