package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.http.AdminSecured;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.MandatoryTransactionService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/mtxs")
@OpenAPIDefinition(info = @Info(description = "Provide methods to operate with mandatory transactions"))
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
@Singleton
public class MandatoryTransactionController {

    @Inject
    @Setter
    private MandatoryTransactionService service;

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"mtxs"}, summary = "Delete mandatory transaction",
            description = "Delete mandatory transaction specified by id",
    security = @SecurityRequirement(name = "admin_api_key"))
    @AdminSecured
    public Response deleteById(@Parameter(description = "Id of mandatory transaction to delete", required = true) @PathParam("id") Long id,
                               @Context HttpServletRequest req) {
        return Response.ok(service.deleteById(id)).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"mtxs"}, summary = "Delete all mandatory transactions",
            description = "Delete from database all mandatory transactions",
    security = @SecurityRequirement(name = "admin_api_key"))
    @AdminSecured
    public Response deleteAll(@Context HttpServletRequest req) {
        return Response.ok(service.clearAll()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"mtxs"}, summary = "Extract mandatory txs",
            description = "Extract mandatory transactions from specified db_id exclusive. Limit is specified by parameter, by default is 100")
    public Response getAll(@Parameter(description = "Db id from which mandatory transactions will be extracted (exclusive). Optional, by default is 0") @DefaultValue ("0") @QueryParam("fromDbId") Long fromDbId,
                           @Parameter(description = "Number of transactions to extract, optional, by default is 100") @DefaultValue("100") @QueryParam("limit") int limit) {
        if (limit < 1 || limit > 100) {
            return ResponseBuilder.apiError(ApiErrors.OUT_OF_RANGE, "limit", 1, 100).build();
        }
        return Response.ok(service.getAll(fromDbId, limit)).build();
    }
}
