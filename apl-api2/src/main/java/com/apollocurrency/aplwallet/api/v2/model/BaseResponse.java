/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;


public class BaseResponse {
    private static final String PROTOCOL_VERSION = "2";
    private String protocol = PROTOCOL_VERSION;
    private Long requestProcessingTime = 0L;

    /**
     * Protocol version
     **/

    @Schema(example = "2", description = "Protocol version")
    @JsonProperty("protocol")
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Time in milliseconds that took from incoming request to response
     **/

    @Schema(example = "12", description = "Time in milliseconds that took from incoming request to response")
    @JsonProperty("requestProcessingTime")
    public Long getRequestProcessingTime() {
        return requestProcessingTime;
    }

    public void setRequestProcessingTime(Long requestProcessingTime) {
        this.requestProcessingTime = requestProcessingTime;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseResponse baseResponse = (BaseResponse) o;
        return Objects.equals(protocol, baseResponse.protocol) &&
            Objects.equals(requestProcessingTime, baseResponse.requestProcessingTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, requestProcessingTime);
    }

    @Override
    public String toString() {

        return "BaseResponse{" +
            "protocol= '" + protocol + '\'' +
            ", requestProcessingTime= " + requestProcessingTime +
            '}';
    }
}
