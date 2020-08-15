package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.model.*;
import com.apollocurrency.aplwallet.api.v2.AccountApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.apollocurrency.aplwallet.api.v2.model.AccountInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.AccountReq;
import com.apollocurrency.aplwallet.api.v2.model.AccountReqSendMoney;
import com.apollocurrency.aplwallet.api.v2.model.AccountReqTest;
import com.apollocurrency.aplwallet.api.v2.model.CreateChildAccountResp;
import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;

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
@Path("/v2/account")


public class AccountApi  {

    @Inject AccountApiService service;

    @POST

    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Returns unsigned CreateChildAccount transaction for creating child accounts", description = "Returns unsigned CreateChildAccount transaction as a byte array. The list of child public keys is attached in the transaction appendix. ", security = {
        @SecurityRequirement(name = "bearerAuth")
    }, tags={ "account" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = CreateChildAccountResp.class))),

        @ApiResponse(responseCode = "401", description = "Unauthorized Error"),

        @ApiResponse(responseCode = "403", description = "Access Forbidden"),

        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response createChildAccountTx(@Parameter(description = "the parent account and the list of public keys" ,required=true) AccountReq body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.createChildAccountTx(body,securityContext);
    }
    @POST
    @Path("/money")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Returns signed SendMoney transaction from child account", description = "Returns signed SendMoney transaction as a byte array. This transaction is a multi-signature signed tx. ", security = {
        @SecurityRequirement(name = "bearerAuth")
    }, tags={ "account" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = CreateChildAccountResp.class))),

        @ApiResponse(responseCode = "401", description = "Unauthorized Error"),

        @ApiResponse(responseCode = "403", description = "Access Forbidden"),

        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response createChildAccountTxSendMony(@Parameter(description = "the parent account, child account and other" ,required=true) AccountReqSendMoney body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.createChildAccountTxSendMony(body,securityContext);
    }
    @POST
    @Path("/test")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Returns signed CreateChildAccount transaction for creating child accounts", description = "Returns signed CreateChildAccount transaction as a byte array. The list of child public keys is attached in the transaction appendix. ", security = {
        @SecurityRequirement(name = "bearerAuth")
    }, tags={ "account" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = CreateChildAccountResp.class))),

        @ApiResponse(responseCode = "401", description = "Unauthorized Error"),

        @ApiResponse(responseCode = "403", description = "Access Forbidden"),

        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response createChildAccountTxTest(@Parameter(description = "the parent account and the list of public keys" ,required=true) AccountReqTest body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.createChildAccountTxTest(body,securityContext);
    }
    @GET
    @Path("/{account}")

    @Produces({ "application/json" })
    @Operation(summary = "Returns the details account information", description = "", tags={ "account" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = AccountInfoResp.class))),

        @ApiResponse(responseCode = "400", description = "Bad request - malformed request or wrong parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "500", description = "Server error - internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response getAccountInfo( @PathParam("account") String account,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getAccountInfo(account,securityContext);
    }
}
