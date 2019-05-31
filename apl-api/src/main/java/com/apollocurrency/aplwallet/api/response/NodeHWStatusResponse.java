/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.NodeHWStatusInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author alukin@gmail.com
 */

public class NodeHWStatusResponse extends ResponseBase{
    /**
     * Message from server side to be displayed
     */
    @Schema(name="BackendStatusResponse", description="Status of node hardware")
    public String message;
    /**
     * Actual backend info
     */  
    public NodeHWStatusInfo backendInfo;
}
