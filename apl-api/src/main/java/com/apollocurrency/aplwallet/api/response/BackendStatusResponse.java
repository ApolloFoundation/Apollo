/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.BackendStatusInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author alukin@gmail.com
 */

public class BackendStatusResponse extends ResponseBase{
    /**
     * Message from server side to be displayed
     */
    @Schema(name="DifferentModel", description="Sample model for the documentation")
    public String message;
    /**
     * Actual backend info
     */  
    public BackendStatusInfo backendInfo;
}
