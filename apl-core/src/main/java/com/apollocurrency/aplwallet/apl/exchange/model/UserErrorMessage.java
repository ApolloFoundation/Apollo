package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserErrorMessage {
    private Long dbId;
    private String address;
    private String error;
    private long timestamp;
}
