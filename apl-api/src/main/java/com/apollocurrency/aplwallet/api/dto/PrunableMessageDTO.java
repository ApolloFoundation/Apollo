package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrunableMessageDTO {
    public String senderRS;
    public String sender;
    public String recipientRS;
    public String recipient;
    public long blockTimestamp;
    public boolean messageIsText;
    public String message;
    public String transaction;
    public boolean isText;
    public long transactionTimestamp;
}
