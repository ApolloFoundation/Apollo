/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;

@Path("/server/info")
public class ServerInfoController {

    public ServerInfoController() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"counts"}, summary = "Retrieve counts values", description = "Get all ")
    public Response counts() {
        return ResponseBuilder.done().build();
    }

}
