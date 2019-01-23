/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.response.BackendStatusResponse;
import com.apollocurrency.aplwallet.apl.core.rest.service.BackendControlService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This endpoint gives info about backend status and allows some control. Should
 * be accessible from localhost only or some other secure whay
 *
 * @author alukin@gmail.com
 */
@Path("/control")
public class BackendControlEndpint {

    private BackendControlService bcService;

    @Inject
    public BackendControlEndpint(BackendControlService bcService) {
        this.bcService = bcService;
    }

    @Path("/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Returns backend status",
            notes = "Returns backend status for each running core"
            + " Status is updated by core on event base",
            response = BackendStatusResponse.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successful execution", response = BackendStatusResponse.class)
    })

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
