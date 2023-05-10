/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.response.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.apl.core.rest.service.DebugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Apollo server info endpoint
 *
 * @author alukin@gmail.com
 */

@Slf4j
@Path("/debug")
public class DebugController {
    private DebugService debugService;

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
    public Response startDownload(@PathParam(value = "id") String id, @QueryParam(value = "password") String password) {

        FileDownloadInfoResponse infoResponse = new FileDownloadInfoResponse();
        FileDownloadInfo fdi = debugService.startFileDownload(id, password);
        infoResponse.downloadInfo = fdi;
        return Response.status(Response.Status.OK).entity(infoResponse).build();
    }
}
