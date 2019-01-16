package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author al
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Base class for all responses. Contains common fields AND error related fields.")
@ApiModel
public class ResponseBase {
    @JsonIgnore
    public static final String PROTOCOL_VERSION = "1";
    /**
     * Protocol version. Should be incremented in case of
     * protocol changes. Incompatible changes should increment
     * major or minor version. Compatible changes should increment
     * revision only.
     */
    @ApiModelProperty(value = "Protocol version")
    public String protocol = PROTOCOL_VERSION;

    /**
     * Error code on new API. 0 means success, no error.
     */
    @ApiModelProperty(value = "Error code for new REST API")
    public Integer newErrorCode;
    /**
     * Time in miliseconds that took from incoming request to
     * responce
     */
    @ApiModelProperty(value = "Request processing time in milliseconds", allowEmptyValue = true)
    public Long requestProcessingTime;
    /**
     * Textual error description, e.g. exception message, etc that could
     * be displayed to user.
     */
    @ApiModelProperty(value = "Textual error description. It can be from message bundle", allowEmptyValue = true)
    public String errorDescription;
    /**
     * Old error code. Should be gone in new API
     */
    @ApiModelProperty(value = "Error code for old REST API. Must be removed in the next protocol version", allowEmptyValue = false)
    public Long errorCode;

    public void setRequestProcessingTime(Long processingTime) {
        this.requestProcessingTime = processingTime;
    }

    public Long getRequestProcessingTime() {
        return requestProcessingTime;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorCode(Long errorCode) {
        this.errorCode = errorCode;
    }

    public Long getErrorCode() {
        return errorCode;
    }

    public Integer getNewErrorCode() {
        return newErrorCode;
    }

    public void setNewErrorCode(Integer newErrorCode) {
        this.newErrorCode = newErrorCode;
    }
}

