package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntryDTO {
    public long ledgerId;
    public boolean isTransactionEvent;
    public String balance;
    public String holdingType;
    public String accountRS;
    public String change;
    public String block;
    public EventType eventType;
    public String event;
    public String account;
    public long height;
    public long timestamp;
    public long requestProcessingTime;
}
