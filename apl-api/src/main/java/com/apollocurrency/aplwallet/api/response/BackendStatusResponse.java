/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.BackendStatusInfo;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author alukin@gmail.com
 */

public class BackendStatusResponse extends ResponseBase{
    /**
     * Message from server side to be displayed
     */
   @ApiModelProperty(value = "Message from server side to be displayed", allowEmptyValue = true)    
   public String message;
    /**
     * Actual backend info
     */  
   @ApiModelProperty(value = "Actual backend info", allowEmptyValue = true)
   public BackendStatusInfo backendInfo;
}
