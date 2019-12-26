/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Base class for all responses
 * @author alukin@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiResponse(responseCode = "200", description = "Successful execution",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ResponseBase.class)))
public class ResponseBase {
    @JsonIgnore
    public static final String PROTOCOL_VERSION = "1";
    /**
     * Protocol version. Should be incremented in case of
     * protocol changes. Incompatible changes should increment
     * major or minor version. Compatible changes should increment
     * revision only.
     */
    @Schema(name="Protocol version", description="Information about Protocol version")
    public String protocol = PROTOCOL_VERSION;

    /**
     * Error code on new API. 0 means success, no error.
     */
    public Integer newErrorCode;
    /**
     * Time in milliseconds that took from incoming request to response
     */
    public Long requestProcessingTime;
    /**
     * Textual error description, e.g. exception message, etc that could
     * be displayed to user.
     */
    public String errorDescription;
    /**
     * Old error code. Should be gone in new API
     */
    public Long errorCode;

    /**
     * Detailed error description with debug information, helpful error data or stacktrace, by default
     * should not be displayed to user. Main purpose: create bug reports with meaningful content.
     */
    public String errorDetails;

    public ResponseBase(Integer newErrorCode, String errorDescription, String errorDetails, Long errorCode) {
        this.newErrorCode = newErrorCode;
        this.errorDescription = errorDescription;
        this.errorDetails = errorDetails;
        this.errorCode = errorCode;
    }

    public ResponseBase(Integer newErrorCode, String errorDescription, Long errorCode) {
        this(newErrorCode, errorDescription, null, errorCode);
    }

    public ResponseBase() {
    }
}
