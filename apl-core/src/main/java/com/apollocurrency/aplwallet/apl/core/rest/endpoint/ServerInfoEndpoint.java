/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.response.ApolloX509Response;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apollo server info endpoint
 * @author alukin@gmail.com
 */
@Path("/serverinfo")
@Api(value = "Server info endpoint: Endpoint for node info", tags = "ServerInfo")
public class ServerInfoEndpoint {
    private static final Logger log = LoggerFactory.getLogger(ServerInfoEndpoint.class);
    private  ServerInfoService siService;
    
    @Inject 
    public ServerInfoEndpoint(ServerInfoService siService) {
        this.siService = siService;
    }
    
    public ServerInfoEndpoint() {
      log.debug("Empty ServerInfoEndpoint created");
    }
    
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Returns node certificate info",
            notes = "Returns block certicates info andcertificates in PEM format."
                    + "Node may have several certificateswith different parameters but with the same VimanaID.",
            response = ApolloX509Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Successful execution", response = ApolloX509Response.class) 
    })
    public Response getX509Info(){
        ApolloX509Response infoResponse = new ApolloX509Response();
        infoResponse.info = siService.getX509Info();
        return Response.status(Response.Status.OK).entity(infoResponse).build();  
    }   
}
