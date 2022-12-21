/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.CacheStatsDTO;
import com.apollocurrency.aplwallet.api.dto.RunningThreadsInfo;
import com.apollocurrency.aplwallet.api.response.CacheStatsResponse;
import com.apollocurrency.aplwallet.api.response.NodeHealthResponse;
import com.apollocurrency.aplwallet.api.response.NodeStatusResponse;
import com.apollocurrency.aplwallet.apl.core.rest.service.BackendControlService;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.google.common.cache.CacheStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * This endpoint gives info about backend status and allows some control. Should
 * be accessible from localhost only or some other secure way
 *
 * @author alukin@gmail.com
 */
@Slf4j
@Singleton
@Path("/control")
public class NodeControlController {
    private BackendControlService bcService;
    private InMemoryCacheManager cacheManager;
    private Converter<CacheStats, CacheStatsDTO> statsConverter;

    /**
     * Empty constructor required by RestEasy
     */
    public NodeControlController() {
        log.debug("Empty BackendControlEndpoint created");
    }

    @Inject
    public NodeControlController(BackendControlService bcService, InMemoryCacheManager cacheManager, Converter<CacheStats, CacheStatsDTO> statsConverter) {
        this.bcService = bcService;
        this.cacheManager = cacheManager;
        this.statsConverter = statsConverter;
    }

    @Path("/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns backend status",
        description = "Returns backend status for software and hardware"
            + " Status is updated by core on event base",
        tags = {"nodecontrol"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NodeStatusResponse.class)))
        }
    )
    @PermitAll
    public Response getBackendStatus(@QueryParam("status") @DefaultValue("All") String state) {
        NodeStatusResponse statusResponse = new NodeStatusResponse();
        statusResponse.nodeInfo = bcService.getNodeStatus();
        statusResponse.tasks = bcService.getNodeTasks(state);
        return Response.status(Response.Status.OK).entity(statusResponse).build();
    }

    @Path("/threads")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns backend threads status",
        description = "Returns backend threads status",
        tags = {"nodecontrol"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RunningThreadsInfo.class)))
        }
    )
    @RolesAllowed("admin")
    public Response getBackendThreads(@QueryParam("adminPassword") @DefaultValue("") String adminPassword) {
        RunningThreadsInfo threadsResponse = bcService.getThreadsInfo();
        return Response.status(Response.Status.OK).entity(threadsResponse).build();
    }

    @Path("/health-full")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns node health info. Protected with admin password",
        description = "Returns complete information about node health "
            + "including DB, P2P, hardware and resource usage",
        tags = {"status"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NodeHealthResponse.class)))
        }
    )
    @RolesAllowed("admin")
    public Response getHealthInfoFull(@QueryParam("adminPassword") @DefaultValue("") String adminPassword) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        NodeHealthResponse infoResponse = new NodeHealthResponse();
        infoResponse.healthInfo = bcService.getNodeHealth();
        infoResponse.statusInfo = bcService.getNodeStatus();
        infoResponse.networkingInfo = bcService.getNetworkingInfo();
        infoResponse.healthInfo.needReboot = !infoResponse.healthInfo.dbOK
            || (infoResponse.networkingInfo.inboundPeers == 0 && infoResponse.networkingInfo.outboundPeers == 0);
        return response.bind(infoResponse).build();
    }

    @Path("/health")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns shorted node health without status info.",
        description = "Returns shorted information node P2P health usage info mainly",
        tags = {"status"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NodeHealthResponse.class)))
        }
    )
    @PermitAll
    public Response getHealthInfo() {
        ResponseBuilder response = ResponseBuilder.startTiming();
        NodeHealthResponse infoResponse = new NodeHealthResponse();
        infoResponse.healthInfo = bcService.getNodeHealth();
        infoResponse.healthInfo.usedDbConnections = null; // remove info
        infoResponse.networkingInfo = bcService.getNetworkingInfo();
        infoResponse.healthInfo.needReboot = !infoResponse.healthInfo.dbOK
            || (infoResponse.networkingInfo.inboundPeers == 0 && infoResponse.networkingInfo.outboundPeers == 0);
        return response.bind(infoResponse).build();
    }

    @Path("/cache")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the statistics about the performance of a cache.",
        description = "Returns the statistics about the performance of a cache."
            + " Cache statistics are incremented according to the following rules:\n" +
            "* When a cache lookup encounters an existing cache entry hitCount is incremented.\n" +
            "* When a cache lookup first encounters a missing cache entry, a new entry is loaded.\n" +
            "   * After successfully loading an entry missCount and loadSuccessCount are incremented, and the total loading time, in nanoseconds, is added to totalLoadTime.\n" +
            "   * When an exception is thrown while loading an entry, missCount and loadExceptionCount are incremented, and the total loading time, in nanoseconds, is added to totalLoadTime.\n" +
            "   * Cache lookups that encounter a missing cache entry that is still loading will wait for loading to complete (whether successful or not) and then increment missCount.\n" +
            "* When an entry is evicted from the cache, evictionCount is incremented.",
        tags = {"status"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CacheStatsResponse.class)))
        }
    )
    @PermitAll
    public Response getCacheStats(@QueryParam("name") @DefaultValue("All") String cache) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        List<CacheStatsDTO> result = new ArrayList<>();
        List<String> cacheNames;
        if (cache.equalsIgnoreCase("all")) {
            cacheNames = cacheManager.getAllocatedCacheNames();
        } else {
            cacheNames = List.of(cache);
        }
        cacheNames.forEach(cacheName -> {
            CacheStats stats = cacheManager.getStats(cacheName);
            if (stats != null) {
                CacheStatsDTO dto = statsConverter.convert(stats);
                dto.setCacheName(cacheName);
                result.add(dto);
            }
        });
        return response.bind(new CacheStatsResponse(result)).build();
    }

}
