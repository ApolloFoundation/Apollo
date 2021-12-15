/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author andrew.zinchenko@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class SmcEventReceipt extends SmcEventResponse {

    public enum Status {
        OK,
        ERROR
    }

    private Status status;
    private String requestId;

    @Builder
    public SmcEventReceipt(Integer errorCode, String errorDescription, Status status, String requestId) {
        super(errorCode, errorDescription, Type.RECEIPT);
        this.status = status;
        this.requestId = requestId;
    }

    public SmcEventReceipt(Status status, String requestId) {
        this(null, null, status, requestId);
    }
}
