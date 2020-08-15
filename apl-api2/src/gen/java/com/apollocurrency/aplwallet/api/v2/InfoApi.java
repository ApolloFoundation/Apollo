package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.model.*;
import com.apollocurrency.aplwallet.api.v2.InfoApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.HealthResponse;

import java.util.Map;
import java.util.List;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;
@Path("/v2/info")


public class InfoApi  {

    @Inject InfoApiService service;

    @GET
    @Path("/health")

    @Produces({ "application/json" })
    @Operation(summary = "Gets node health", description = "", tags={ "info" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = HealthResponse.class))),

        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getHealthInfo(@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getHealthInfo(securityContext);
    }
}
