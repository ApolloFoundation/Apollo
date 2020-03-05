/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.apl.core.rest.service.DebugService;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apollo server info endpoint
 * @author alukin@gmail.com
 */

@Path("/debug")
public class DebugController {
    private static final Logger log = LoggerFactory.getLogger(DebugController.class);
    private  DebugService debugService;

    @Inject
    public DebugController(DebugService service) {
        this.debugService = service;
    }

    public DebugController() {
      log.debug("Empty DebugEndpoint created");
    }

    @Path("/downloadstart/${id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Starts file download from peers",
            description = "Starts file download from peers",
            tags = {"debug"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FileDownloadInfoResponse.class)))
            }
    )
    @PermitAll
    public Response startDownload(@PathParam(value = "id") String id, @QueryParam(value="password") String password){
        
        FileDownloadInfoResponse infoResponse = new FileDownloadInfoResponse();
        FileDownloadInfo fdi = debugService.startFileDownload(id,password);
        infoResponse.downloadInfo=fdi;
        return Response.status(Response.Status.OK).entity(infoResponse).build();
    }
}
