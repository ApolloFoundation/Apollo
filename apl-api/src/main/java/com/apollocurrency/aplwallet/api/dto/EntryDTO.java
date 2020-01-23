package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntryDTO extends BaseDTO {
    private Long ledgerId;
    private boolean isTransactionEvent;
    private String balance;
    private String holdingType;
    private String accountRS;
    private String change;
    private String block;
    private EventType eventType;
    private String event;
    private String account;
    private Long height;
    private Long timestamp;
}
