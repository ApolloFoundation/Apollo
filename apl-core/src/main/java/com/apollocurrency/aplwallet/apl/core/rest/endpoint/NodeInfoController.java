/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.response.ApolloX509Response;
import com.apollocurrency.aplwallet.api.response.NodeForgersResponse;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Apollo server info endpoint
 * @author alukin@gmail.com
 */

@Path("/nodeinfo")
public class NodeInfoController {
    private static final Logger log = LoggerFactory.getLogger(NodeInfoController.class);
    private  ServerInfoService siService;

    @Inject
    public NodeInfoController(ServerInfoService siService) {
        this.siService = siService;
    }

    public NodeInfoController() {
      log.debug("Empty ServerInfoEndpoint created");
    }

    @Path("/x509")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns node certificate info",
            description = "Returns block certicates info andcertificates in PEM format."
                    + "Node may have several certificateswith different parameters but with the same VimanaID.",
            tags = {"status"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ApolloX509Response.class)))
            }
    )
    public Response getX509Info(){
        ApolloX509Response infoResponse = new ApolloX509Response();
        infoResponse.info = siService.getX509Info();
        return Response.status(Response.Status.OK).entity(infoResponse).build();
    }

    @Path("/forgers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns node's active forgers",
            description = "Returns node's active forgers with some aparameters",
            tags = {"status"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = NodeForgersResponse.class)))
            }
    )
    public Response getActiveForgers() {
        NodeForgersResponse resp = new NodeForgersResponse();
        resp.setGenerators(siService.getActiveForgers(false));
        return Response.status(Response.Status.OK).entity(resp).build();
    }
}
