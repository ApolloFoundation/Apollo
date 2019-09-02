/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.RunningThreadsInfo;
import com.apollocurrency.aplwallet.api.response.ApolloX509Response;
import com.apollocurrency.aplwallet.api.response.NodeHealthResponse;
import com.apollocurrency.aplwallet.api.response.NodeStatusResponse;
import com.apollocurrency.aplwallet.apl.core.rest.service.BackendControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This endpoint gives info about backend status and allows some control. Should
 * be accessible from localhost only or some other secure way
 *
 * @author alukin@gmail.com
 */
@Path("/control")
public class NodeControlController {
    private static final Logger log = LoggerFactory.getLogger(NodeControlController.class);

    private BackendControlService bcService;
    /**
     * Empty constructor re quired by REstEasy
     */


    public NodeControlController() {
       log.debug("Empty BackendControlEndpoint created"); 
    }

    @Inject
    public NodeControlController(BackendControlService bcService) {
        this.bcService = bcService;
    }
    
    @Path("/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns backend status",
            description = "Returns backend status for software and hardware"
            + " Status is updated by core on event base",
            tags = {"nodecontrol"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = NodeStatusResponse.class)))
            }
    )
    public Response getBackendStatus(@QueryParam("status") @DefaultValue("All") String state) {
        NodeStatusResponse statusResponse = new NodeStatusResponse();
        statusResponse.nodeInfo = bcService.getNodeStatus();
        statusResponse.tasks = bcService.getNodeTasks(state);
        return Response.status(Response.Status.OK).entity(statusResponse).build();
    }
    
    @Path("/threads")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns backend threads status",
            description = "Returns backend threads status",
            tags = {"nodecontrol"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RunningThreadsInfo.class)))
            }
    )
    public Response getBackendThreads(@Context HttpServletRequest request, @QueryParam("adminPassword") String password) {
        boolean passwordOK = bcService.isAdminPasswordOK(request);
        if(passwordOK){
            RunningThreadsInfo threadsResponse=bcService.getThreadsInfo();
            return Response.status(Response.Status.OK).entity(threadsResponse).build();
        }else{
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @Path("/health")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns node health info. Protected with admin password",
            description = "Returns complete information about node health"
                    + "includind DB, P2P, hardware and resource usage",
            tags = {"status"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ApolloX509Response.class)))
            }
    )
    public Response getHealthInfo(@Context HttpServletRequest request, @QueryParam("adminPassword") String password) {
        boolean passwordOK = bcService.isAdminPasswordOK(request);
        if (passwordOK) {
            NodeHealthResponse infoResponse = new NodeHealthResponse();
            infoResponse.healthInfo = bcService.getNodeHealth();
            infoResponse.statusInfo = bcService.getNodeStatus();
            infoResponse.networkingInfo = bcService.getNetworkingInfo();
            infoResponse.healthInfo.needReboot = !infoResponse.healthInfo.dbOK
                    || (infoResponse.networkingInfo.inboundPeers == 0 && infoResponse.networkingInfo.outboundPeers == 0);
            return Response.status(Response.Status.OK).entity(infoResponse).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
}
