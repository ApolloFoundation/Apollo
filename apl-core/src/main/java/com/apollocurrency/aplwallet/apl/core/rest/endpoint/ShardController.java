/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.apl.core.rest.converter.ShardToDtoConverter;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
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
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
@OpenAPIDefinition(info = @Info(description = "Provide shard information and manipulation methods"))
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
@Path("/shards")
@Singleton
public class ShardController {

    private ShardService shardService;
    private ShardToDtoConverter shardConverter = new ShardToDtoConverter();
    public static int maxAPIFetchRecords;

    @Inject
    public ShardController(ShardService shardService, @Property(name = "apl.maxAPIRecords", defaultValue = "100") int maxAPIrecords) {
        this.shardService = shardService;
        maxAPIFetchRecords = maxAPIrecords;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve all completed only shards",
        description = "Get all 'completed' shard entries from database",
        tags = {"shards"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ShardDTO.class)))
        })
    @PermitAll
    public Response getAllShards() {
        List<ShardDTO> allCompletedShards = shardService.getAllCompletedShards().stream()
            .map(shard -> shardConverter.convert(shard)).collect(Collectors.toList());
        return Response.ok(allCompletedShards).build();
    }

    @POST
    @Path("/reset/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Reset node to backup db before sharding specified by id",
        description = "Find backup before shard with specified id and when it exists - we will drop our blockchain and load backup entirely",
        security = @SecurityRequirement(name = "admin_api_key"),
        tags = {"shards"}
    )
    @RolesAllowed("admin")
    public Response resetToBackup(@PathParam("id") long shardId) {
        return Response.ok(shardService.reset(shardId)).build();
    }

    @GET
    @Path("/blocks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Select full shards containing Blocks requested by 'firstIndex' and 'lastIndex' height range",
        description = "Select full shards containing Blocks requested by 'firstIndex' and 'lastIndex' height range values."
            +" Index range is recalculated by default number of blocks to be requested using 'apl.maxAPIRecords' config value",
        tags = {"shards"},
        responses = {
        @ApiResponse(responseCode = "200", description = "Successful execution",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ShardDTO.class)))
    }    )
    @PermitAll
    public Response getBlocksFromShards(
        @Parameter(description = "A zero-based index to the 'first height' to retrieve (optional)." )
        @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the 'last height' to retrieve (optional)." )
        @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex
    ) {
//        FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);
        log.debug("getBlocksFromShards( firstIndex = {}, lastIndex = {} ) = ", firstIndex, lastIndex);
        List<ShardDTO> foundShardsWithBlock = shardService
//            .getShardsByBlockHeightRange(flIndex.getFirstIndex(), flIndex.getLastIndex()).stream() // recalculated
            .getShardsByBlockHeightRange(firstIndex, lastIndex).stream() // straight values
            .map(shard -> shardConverter.convert(shard)).collect(Collectors.toList());
        return Response.ok(foundShardsWithBlock).build();
    }

}
