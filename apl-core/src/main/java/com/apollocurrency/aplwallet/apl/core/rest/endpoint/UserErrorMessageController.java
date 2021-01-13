package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.service.state.UserErrorMessageService;
import com.apollocurrency.aplwallet.apl.dex.core.model.UserErrorMessage;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/user-errors")
@OpenAPIDefinition(info = @Info(description = "Provide methods to operate with user errors"))
@Singleton
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
public class UserErrorMessageController {

    private UserErrorMessageService service;


    public UserErrorMessageController() {
    }

    @Inject
    public UserErrorMessageController(UserErrorMessageService service) {
        this.service = service;
    }

    @Path("/{address}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"user-errors"}, summary = "Return last user errors for specified user",
        description = "Extract descending ordered user errors to specified db_id exclusive. Limit and user address are specified by parameters")
    public Response getForUser(@Parameter(description = "db id to which user-errors should be extracted. By default is Long.MAX_VALUE") @QueryParam("toDbId") @DefaultValue("" + Long.MAX_VALUE) long toDbId,
                               @Parameter(description = "Number of entries to extract. By default is 100. Cannot be greater than 100.") @QueryParam("limit") @DefaultValue("100") int limit,
                               @Parameter(description = "User address", required = true) @PathParam("address") String address
    ) {
        int correctedLimit = Math.min(100, limit);
        return Response.ok(service.getAllByAddress(address, toDbId, correctedLimit)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"user-errors"}, summary = "Return last user errors for all users",
        description = "Extract descending ordered user errors until the specified db_id reached. Limit is specified by parameter",
        security = @SecurityRequirement(name = "admin_api_key"),
        responses = @ApiResponse(description = "User error message representation. Timestamp corresponds to UTC time.", responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserErrorMessage.class))))
    @RolesAllowed("admin")
    public Response getAll(@Parameter(description = "db id to which user-errors should be extracted. By default is Long.MAX_VALUE") @QueryParam("toDbId") @DefaultValue("" + Long.MAX_VALUE) long toDbId,
                           @Parameter(description = "Number of entries to extract. By default is 100.") @QueryParam("limit") @DefaultValue("100") int limit) {
        int correctedLimit = Math.max(0, limit);
        return Response.ok(service.getAll(toDbId, correctedLimit)).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"user-errors"}, summary = "Delete user errors for all users",
        description = "Will delete all user errors before specified timestamp",
        security = @SecurityRequirement(name = "admin_api_key"),
        responses = @ApiResponse(description = "Number of deleted entries", responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserErrorsDeleteResponse.class))))
    @RolesAllowed("admin")
    public Response deleteByTimestamp(@Parameter(description = "Timestamp before which all user errors should be deleted ", required = true) @QueryParam("timestamp") long timestamp) {
        return Response.ok(new UserErrorsDeleteResponse(service.deleteByTimestamp(timestamp))).build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UserErrorsDeleteResponse {
        private int deleted;
    }
}
