/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.api.dto.NodeStatusInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Response that contains node status including
 * running tasks and other information
 * @author alukin@gmail.com
 */
@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiResponse(responseCode = "200", description = "Successful execution",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = NodeStatusResponse.class)))
public class NodeStatusResponse extends ResponseBase {
    public String message = "";
    public List<DurableTaskInfo> tasks = new ArrayList<>();
    public NodeStatusInfo nodeInfo = new NodeStatusInfo();
}
