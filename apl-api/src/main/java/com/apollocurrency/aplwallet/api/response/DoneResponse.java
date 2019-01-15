
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Simple response when something was done successfully.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel("Respond informs about 'success' or 'failure' of operation")
@ApiModel
public class DoneResponse extends ResponseBase {
    @ApiModelProperty("True if process was started successfully")
    public Boolean done;

    @Override
    public String toString() {
        return "DoneResponse{" +
                "done='" + done + "'}";
    }
}
