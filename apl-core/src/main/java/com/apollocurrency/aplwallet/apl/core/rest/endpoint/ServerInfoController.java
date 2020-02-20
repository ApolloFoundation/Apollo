/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Objects;

import com.apollocurrency.aplwallet.api.dto.info.AccountsCountDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.apl.core.rest.RestParameters;
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
    public Response counts(
        @Parameter(name = "numberOfAccounts", description = "number Of returned Accounts", allowEmptyValue = true)
            @QueryParam("numberOfAccounts") String numberOfAccountsStr
    ) {
        log.debug("Started counts : \t'numberOfAccounts' = {}", numberOfAccountsStr);
        ResponseBuilder response = ResponseBuilder.startTiming();
        int numberOfAccounts = RestParameters.parseInt(numberOfAccountsStr, "numberOfAccounts",
            Constants.MIN_TOP_ACCOUNTS_NUMBER, Constants.MAX_TOP_ACCOUNTS_NUMBER, false);
        int numberOfAccountsMax = Math.max(numberOfAccounts, Constants.MIN_TOP_ACCOUNTS_NUMBER);

        AccountsCountDto dto = serverInfoService.getAccountsStatistic(numberOfAccountsMax);
        log.debug("counts result : {}", dto);
        return response.bind(dto).build();
    }

    @Path("/blockchain/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns statistics Account information",
        description = "Returns statistics information about specified count of account",
        tags = {"info"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockchainStatusDto.class)))
        }
    )
    public Response blockchainStatus() {
        ResponseBuilder response = ResponseBuilder.startTiming();
        BlockchainStatusDto dto = serverInfoService.getBlockchainStatus();
        log.debug("counts result : {}", dto);
        return response.bind(dto).build();
    }

}
