package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.model.*;
import com.apollocurrency.aplwallet.api.v2.TransactionApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.ListResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.api.v2.model.TxRequest;

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
@Path("/v2/transaction")


public class TransactionApi  {

    @Inject TransactionApiService service;

    @POST
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Broadcast transaction and return transaction receipt.", description = "Asynchronously broadcast the signed transaction to the network.   The transaction is validated and immediately put into an unconfirmed transaction pool   for further sending to the blockchain. ", tags={ "tx" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = TxReceipt.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "409", description = "The request could not be completed due to a conflict with the current state of the resource. The resource is busy and the request might be reissued later. "),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response broadcastTx(@Parameter(description = "the signed transaction is a byte array in hex format" ,required=true) TxRequest body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.broadcastTx(body,securityContext);
    }
    @POST
    @Path("/batch")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Broadcast a batch of transactions and return the transaction receipt list.", description = "Asynchronously broadcast the batch of the signed transaction to the network. The transactions are immediately put into an unconfirmed transaction pool in the same order as in the batch and later are sending to the network.", security = {
        @SecurityRequirement(name = "bearerAuth")
    }, tags={ "tx" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = ListResponse.class))),
        
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        
        @ApiResponse(responseCode = "403", description = "Access token does not have the required scope"),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response broadcastTxBatch(@Parameter(description = "an array of signed transactions, each item is a byte array in hex format" ,required=true) java.util.List<TxRequest> body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.broadcastTxBatch(body,securityContext);
    }
    @GET
    @Path("/{transaction}")
    
    @Produces({ "application/json" })
    @Operation(summary = "Get transaction object given transaction id.", description = "Return the detail information about transaction by id.", tags={ "tx" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = TransactionInfoResp.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        
        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getTxById( @PathParam("transaction") String transaction,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getTxById(transaction,securityContext);
    }
}
