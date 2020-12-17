/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.info.BlockchainConstantsDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStateDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.api.dto.info.TimeDto;
import com.apollocurrency.aplwallet.api.dto.info.TotalSupplyDto;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Path("/server")
public class ServerInfoController {
    @Context
    SecurityContext context;
    private ServerInfoService serverInfoService;

    @Inject
    public ServerInfoController(ServerInfoService serverInfoService) {
        this.serverInfoService = Objects.requireNonNull(serverInfoService, "serverInfoService is NULL");
    }

    public ServerInfoController() {
    }

    @Path("/blockchain/status")
    @GET // for backward compatibility
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns status information",
        description = "Returns status information about node settings",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockchainStatusDto.class)))
        }
    )
    @PermitAll
    public Response blockchainStatusGet() {
        return getResponseStatus();
    }

    @Path("/blockchain/status")
    @POST // for backward compatibility
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns status information",
        description = "Returns status information about node settings",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockchainStatusDto.class)))
        }
    )
    @PermitAll
    public Response blockchainStatusPost() {
        return getResponseStatus();
    }

    private Response getResponseStatus() {
        log.trace("Started blockchain Status");
        ResponseBuilder response = ResponseBuilder.startTiming();
        BlockchainStatusDto dto = serverInfoService.getBlockchainStatus();
        log.trace("blockchain Status result : {}", dto);
        return response.bind(dto).build();
    }

    @Path("/blockchain/constants")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns constants information",
        description = "Returns constants  information on current node in run-time",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockchainConstantsDto.class)))
        }
    )
    @PermitAll
    public Response blockchainConstants() {
        log.trace("Started blockchain Constants");
        ResponseBuilder response = ResponseBuilder.startTiming();
        BlockchainConstantsDto dto = serverInfoService.getBlockchainConstants();
        log.trace("blockchain Constants result : {}", dto);
        return response.bind(dto).build();
    }

    @Path("/blockchain/state")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns status + additional data field information",
        description = "Returns status + additional data field information for node",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockchainStateDto.class)))
        }
    )
    @RolesAllowed("admin")
    public Response blockchainState(
        @Parameter(name = "includeCounts", description = "true for including additional data", allowEmptyValue = true)
        @QueryParam("includeCounts") Boolean includeCounts,
        @Parameter(description = "The admin password.", required = true) @QueryParam("adminPassword") String adminPassword
    ) {
        log.trace("Started blockchain State: \t includeCounts = {}", includeCounts);
        ResponseBuilder response = ResponseBuilder.startTiming();
        // that dto is BlockchainStatusDto + additional fields in BlockchainStateDto
        BlockchainStateDto dto = serverInfoService.getBlockchainState(includeCounts);
        log.trace("blockchain State result : {}", dto);
        return response.bind(dto).build();
    }

    @Path("/blockchain/time")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns current Time information",
        description = "Returns current Time information for node",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TimeDto.class)))
        }
    )
    @PermitAll
    public Response blockchainTime() {
        log.trace("Started blockchain Time");
        ResponseBuilder response = ResponseBuilder.startTiming();
        TimeDto dto = serverInfoService.getTime();
        log.trace("blockchain Constants result : {}", dto);
        return response.bind(dto).build();
    }

    @Path("/blockchain/supply")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns Total Supply information",
        description = "Returns total supply information for node",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TotalSupplyDto.class)))
        }
    )
    @PermitAll
    public Response blockchainTotalSupply() {
        log.trace("Started Total Supply");
        ResponseBuilder response = ResponseBuilder.startTiming();
        TotalSupplyDto dto = serverInfoService.getTotalSupply();
        log.trace("blockchain Total Supply result : {}", dto);
        return response.bind(dto).build();
    }

    @Path("/blockchain/properties")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns all node properties",
        description = "Returns all node properties",
        tags = {"info"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful execution",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class))),

        @ApiResponse(responseCode = "401", description = "Unauthorized Error"),

        @ApiResponse(responseCode = "403", description = "Access Forbidden"),

    }
    )
    @RolesAllowed("admin")
    public Response blockchainProperties(
        @Parameter(description = "The admin password.") @QueryParam("adminPassword") String adminPassword
    ) {
        log.trace("Started get Properties");
        Map<String, Object> dto = serverInfoService.getProperties();
        log.trace("blockchain get Properties result : {}", dto);
        return Response.status(Response.Status.OK).entity(dto).build();
    }

}
