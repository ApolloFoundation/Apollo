package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.model.*;
import com.apollocurrency.aplwallet.api.v2.StateApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.apollocurrency.aplwallet.api.v2.model.BlockInfo;
import com.apollocurrency.aplwallet.api.v2.model.BlockchainInfo;
import com.apollocurrency.aplwallet.api.v2.model.CountResponse;
import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionInfoArrayResp;
import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;

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
@Path("/v2/state")


public class StateApi  {

    @Inject StateApiService service;

    @GET
    @Path("/block")
    
    @Produces({ "application/json" })
    @Operation(summary = "Get block object", description = "Return details information about the block given block height. If no height is provided, it will fetch the latest block.", tags={ "state" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = BlockInfo.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getBlockByHeight(  @DefaultValue("-1") @QueryParam("height") String height,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getBlockByHeight(height,securityContext);
    }
    @GET
    @Path("/block/{block}")
    
    @Produces({ "application/json" })
    @Operation(summary = "Get block object given block id", description = "Return the detail information about block given id", tags={ "state" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = BlockInfo.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getBlockById( @PathParam("block") String block,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getBlockById(block,securityContext);
    }
    @GET
    @Path("/blockchain")
    
    @Produces({ "application/json" })
    @Operation(summary = "Get blockchain object", description = "Return details information about the blockchain", tags={ "state" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = BlockchainInfo.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getBlockchainInfo(@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getBlockchainInfo(securityContext);
    }
    @GET
    @Path("/tx/{transaction}")
    
    @Produces({ "application/json" })
    @Operation(summary = "Get transaction receipt given transaction id.", description = "Return the brief information about transaction given id.", tags={ "state" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = TxReceipt.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getTxReceiptById( @PathParam("transaction") String transaction,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getTxReceiptById(transaction,securityContext);
    }
    @POST
    @Path("/tx")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Get transaction receipt list given list of the transaction id.", description = "Return the list of the brief information about transactions.", tags={ "state" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TxReceipt.class)))),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getTxReceiptList(@Parameter(description = "the list of transaction id" ,required=true) java.util.List<String> body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getTxReceiptList(body,securityContext);
    }
    @GET
    @Path("/unconfirmed")
    
    @Produces({ "application/json" })
    @Operation(summary = "Get list of unconfirmed transaction receipts.", description = "Return the list of the detail information about unconfirmed transactions.", security = {
        @SecurityRequirement(name = "bearerAuth")
    }, tags={ "state" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = TransactionInfoArrayResp.class))),
        
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        
        @ApiResponse(responseCode = "403", description = "Access token does not have the required scope"),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getUnconfirmedTx(  @DefaultValue("1") @QueryParam("page") Integer page, @Max(100)  @DefaultValue("50") @QueryParam("perPage") Integer perPage,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getUnconfirmedTx(page,perPage,securityContext);
    }
    @GET
    @Path("/unconfirmed/count")
    
    @Produces({ "application/json" })
    @Operation(summary = "Get count of unconfirmed transaction receipts.", description = "Return the count of unconfirmed transactions.", tags={ "state" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = CountResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getUnconfirmedTxCount(@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getUnconfirmedTxCount(securityContext);
    }
}
