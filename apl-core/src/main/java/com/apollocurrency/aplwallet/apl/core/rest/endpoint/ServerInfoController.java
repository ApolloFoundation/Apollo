/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.util.Map;
import java.util.Objects;

import com.apollocurrency.aplwallet.api.dto.info.AccountsCountDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainConstantsDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStateDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.api.dto.info.TimeDto;
import com.apollocurrency.aplwallet.api.dto.info.TotalSupplyDto;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/server")
public class ServerInfoController {
    private ServerInfoService serverInfoService;
    @Context SecurityContext context;

    @Inject
    public ServerInfoController(ServerInfoService serverInfoService) {
        this.serverInfoService = Objects.requireNonNull(serverInfoService,"serverInfoService is NULL");
    }

    public ServerInfoController() {
    }

    @Path("/info/count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns statistics Account information",
        description = "Returns statistics information about specified count of account",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AccountsCountDto.class)))
        }
    )
    @PermitAll
    public Response counts(
        @Parameter(name = "numberOfAccounts", description = "number Of returned Accounts", allowEmptyValue = true)
            @QueryParam("numberOfAccounts") @Min(Constants.MIN_TOP_ACCOUNTS_NUMBER) @Max(Constants.MAX_TOP_ACCOUNTS_NUMBER)
                @DefaultValue("50") Integer numberOfAccounts
    ) {
        log.debug("Started counts : \t'numberOfAccounts' = {}", numberOfAccounts);
        ResponseBuilder response = ResponseBuilder.startTiming();
        int numberOfAccountsMax = Math.max(numberOfAccounts, Constants.MIN_TOP_ACCOUNTS_NUMBER);

        AccountsCountDto dto = serverInfoService.getAccountsStatistic(numberOfAccountsMax);
        log.debug("counts result : {}", dto);
        return response.bind(dto).build();
    }

    @Path("/blockchain/status")
    @GET
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
    public Response blockchainStatus() {
        log.debug("Started blockchain Status");
        ResponseBuilder response = ResponseBuilder.startTiming();
        BlockchainStatusDto dto = serverInfoService.getBlockchainStatus();
        log.debug("blockchain Status result : {}", dto);
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
        log.debug("Started blockchain Constants");
        ResponseBuilder response = ResponseBuilder.startTiming();
        BlockchainConstantsDto dto = serverInfoService.getBlockchainConstants();
        log.debug("blockchain Constants result : {}", dto);
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
        log.debug("Started blockchain State: \t includeCounts = {}", includeCounts);
        ResponseBuilder response = ResponseBuilder.startTiming();
        // that dto is BlockchainStatusDto + additional fields in BlockchainStateDto
        BlockchainStateDto dto = serverInfoService.getBlockchainState(includeCounts);
        log.debug("blockchain State result : {}", dto);
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
        log.debug("Started blockchain Time");
        ResponseBuilder response = ResponseBuilder.startTiming();
        TimeDto dto = serverInfoService.getTime();
        log.debug("blockchain Constants result : {}", dto);
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
        log.debug("Started Total Supply");
        ResponseBuilder response = ResponseBuilder.startTiming();
        TotalSupplyDto dto = serverInfoService.getTotalSupply();
        log.debug("blockchain Total Supply result : {}", dto);
        return response.bind(dto).build();
    }

    @Path("/blockchain/properties")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns all node properties",
        description = "Returns all node properties",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Map.class)))
        }
    )
    @RolesAllowed("admin")
    public Response blockchainProperties(
        @Parameter(description = "The admin password.") @QueryParam("adminPassword") String adminPassword
    ) {
        log.debug("Started get Properties");
        Map<String, Object> dto = serverInfoService.getProperties();
        log.debug("blockchain get Properties result : {}", dto);
        return Response.status(Response.Status.OK).entity(dto).build();
    }

}
