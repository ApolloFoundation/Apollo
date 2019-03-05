/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.response.BackendStatusResponse;
import com.apollocurrency.aplwallet.apl.core.rest.service.BackendControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This endpoint gives info about backend status and allows some control. Should
 * be accessible from localhost only or some other secure whay
 *
 * @author alukin@gmail.com
 */
@Path("/control")
public class BackendControlEndpoint {
    private static final Logger log = LoggerFactory.getLogger(BackendControlEndpoint.class);
    private BackendControlService bcService;
    /**
     * Empty constructor re quired by REstEasy
     */


    public BackendControlEndpoint() {
       log.debug("Empty BackendControlEndpoint created"); 
    }

    @Inject
    public BackendControlEndpoint(BackendControlService bcService) {
        this.bcService = bcService;
    }

    @Path("/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns backend status",
            description = "Returns backend status for each running core"
            + " Status is updated by core on event base",
            tags = {"blockchain"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BackendStatusResponse.class)))
            }
    )
    public Response getBackendStatus(@QueryParam("detailed") @DefaultValue("false") Boolean detailed) {
        BackendStatusResponse statusResponse = new BackendStatusResponse();
        statusResponse.message = "Seems that server is OK :) TODO: fill with resl data";
        statusResponse.backendInfo = bcService.getStatus();
        if (detailed) {
            statusResponse.message += " Details: shit happens sometimes.";
        }
        return Response.status(Response.Status.OK).entity(statusResponse).build();
    }
}
