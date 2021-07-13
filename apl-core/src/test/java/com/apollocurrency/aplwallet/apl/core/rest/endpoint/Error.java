/*
 *  Copyright © 2018-2021 Apollo Foundation
 */


package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import lombok.Data;

@Data
public class Error {
    private String errorDescription;
    private int errorCode;
    private int protocol;
    private int newErrorCode;
    private int requestProcessingTime;
}
