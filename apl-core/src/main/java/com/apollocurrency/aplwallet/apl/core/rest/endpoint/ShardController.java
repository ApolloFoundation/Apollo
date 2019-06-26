/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/shards")
@OpenAPIDefinition(tags = {@Tag(name = "/shards")}, info = @Info(description = "Provide data about shards"))
@Singleton
public class ShardController {
    private ShardService shardService;

    @Inject
    public ShardController(ShardService shardService) {
        this.shardService = shardService;
    }

    public ShardController() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"shards"}, summary = "Retrieve all completed only shards", description = "Get all 'completed' shard entries from database")
    public Response getAllShards() {
        return Response.ok(shardService.getAllCompletedShards()).build();
    }
}
