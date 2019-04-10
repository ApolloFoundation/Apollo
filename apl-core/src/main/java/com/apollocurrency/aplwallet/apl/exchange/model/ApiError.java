package com.apollocurrency.aplwallet.apl.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


public class ApiError {

    private String errorDescription;
    private Integer errorCode;


    public ApiError(String errorDescription, Integer errorCode) {
        this.errorDescription = errorDescription;
        this.errorCode = errorCode;
    }

    /**
     * Error description
     **/

    @ApiModelProperty(value = "Error description")
    @JsonProperty("errorDescription")
    public String getErrorDescription() {
        return errorDescription;
    }
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    /**
     * Error Code
     **/

    @ApiModelProperty(value = "Error Code")
    @JsonProperty("errorCode")
    public Integer getErrorCode() {
        return errorCode;
    }
    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiError error = (ApiError) o;
        return Objects.equals(errorDescription, error.errorDescription) &&
                Objects.equals(errorCode, error.errorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorDescription, errorCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Error {\n");

        sb.append("    errorDescription: ").append(toIndentedString(errorDescription)).append("\n");
        sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
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