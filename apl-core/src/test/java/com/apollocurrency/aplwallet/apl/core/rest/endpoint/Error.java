package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import lombok.Data;

@Data
public class Error {
    private String errorDescription;
    private int errorCode;
}
