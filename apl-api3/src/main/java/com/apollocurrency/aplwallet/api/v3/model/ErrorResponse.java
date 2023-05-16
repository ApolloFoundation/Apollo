/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class ErrorResponse extends BaseResponse {
    private Integer errorCode;
    private String errorDescription;
    private String errorDetails;

    /**
     * Error code on new API. null or 0 means success, no error.
     **/

    @Schema(example = "12", description = "Error code on new API. null or 0 means success, no error.")
    @JsonProperty("errorCode")
    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Textual error description, e.g. exception message, etc that could be displayed to user
     **/

    @Schema(example = "The mandatory parameter 'id' is not specified.", description = "Textual error description, e.g. exception message, etc that could be displayed to user")
    @JsonProperty("errorDescription")
    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    /**
     * Detailed error description with debug information, helpful error data or stacktrace, by default should not be displayed to user. Main purpose is create bug reports with meaningful content.
     **/

    @Schema(example = "Description of failure", description = "Detailed error description with debug information, helpful error data or stacktrace, by default should not be displayed to user. Main purpose is create bug reports with meaningful content. ")
    @JsonProperty("errorDetails")
    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ErrorResponse errorResponse = (ErrorResponse) o;
        return Objects.equals(errorCode, errorResponse.errorCode) &&
            Objects.equals(errorDescription, errorResponse.errorDescription) &&
            Objects.equals(errorDetails, errorResponse.errorDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, errorDescription, errorDetails, super.hashCode());
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
            "errorCode=" + errorCode +
            ", errorDescription='" + errorDescription + '\'' +
            ", errorDetails='" + errorDetails + '\'' +
            ", super=" + super.toString() +
            '}';
    }
}
