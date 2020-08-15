package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.model.*;
import com.apollocurrency.aplwallet.api.v2.OperationApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.QueryCountResult;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.apollocurrency.aplwallet.api.v2.model.QueryResult;

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
@Path("/v2/operation")


public class OperationApi  {

    @Inject OperationApiService service;

    @POST
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Returns the operation list corresponding the query object group by account", description = "", security = {
        @SecurityRequirement(name = "bearerAuth")
    }, tags={ "operations" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = QueryResult.class))),
        
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        
        @ApiResponse(responseCode = "403", description = "Access token does not have the required scope"),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getOperations(@Parameter(description = "the query object" ,required=true) QueryObject body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getOperations(body,securityContext);
    }
    @POST
    @Path("/count")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Returns the count of operations corresponding the query object group by account", description = "", security = {
        @SecurityRequirement(name = "bearerAuth")
    }, tags={ "operations" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = QueryCountResult.class))),
        
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        
        @ApiResponse(responseCode = "403", description = "Access token does not have the required scope"),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getOperationsCount(@Parameter(description = "the query object" ,required=true) QueryObject body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getOperationsCount(body,securityContext);
    }
}
