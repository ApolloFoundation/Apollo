/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.v2.model;

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
    public boolean equals(java.lang.Object o) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("class ErrorResponse {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
        sb.append("    errorDescription: ").append(toIndentedString(errorDescription)).append("\n");
        sb.append("    errorDetails: ").append(toIndentedString(errorDetails)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
