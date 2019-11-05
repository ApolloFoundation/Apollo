package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserErrorMessage {
    private Long dbId;
    private String address;
    private String error;
    private String operation;
    private String details;
    private long timestamp;
}
