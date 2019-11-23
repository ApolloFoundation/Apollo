package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountMessageDTO extends ResponseBase {
    private String senderRS;
    private String sender;
    private String recipientRS;
    private String recipient;
    private Long blockTimestamp;
    private boolean messageIsText;
    private String message;
    private String transaction;
    private boolean isText;
    private Long transactionTimestamp;
}
