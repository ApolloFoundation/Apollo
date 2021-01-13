/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.response.BlocksResponse;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.FirstLastIndexBeanParam;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.LongParameter;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidBlockchainHeight;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidTimestamp;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@Slf4j
@OpenAPIDefinition(info = @Info(description = "Provide several block retrieving methods"))
@Singleton
@Path("/block")
public class BlockController {
    private Blockchain blockchain;
    private BlockConverter blockConverter;
    public static int maxAPIFetchRecords;
    private TimeService timeService;

    @Inject
    public BlockController(Blockchain blockchain, BlockConverter blockConverter,
                           @Property(name = "apl.maxAPIRecords", defaultValue = "100") int maxAPIrecords, TimeService timeService) {
        this.blockchain = Objects.requireNonNull(blockchain);
        this.blockConverter = Objects.requireNonNull(blockConverter);
        maxAPIFetchRecords = maxAPIrecords;
        this.timeService = Objects.requireNonNull(timeService);
    }

    @Path("/one")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns Block information",
        description = "The API returns Block information with transaction info depending on specified params",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockDTO.class)))
        })
    @PermitAll
    public Response getBlock(
        @Parameter(description = "Block id (optional, default is last block)",
            schema = @Schema(implementation = Long.class, description="Block id (optional, default is last block"))
            @QueryParam("block") LongParameter blockId,
        @Parameter(description = "The block height (optional, default is last block).")
            @QueryParam("height") @DefaultValue("-1") @ValidBlockchainHeight int height,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp,
        @Parameter(description = "Include transactions detail info" )
            @QueryParam("includeTransactions") @DefaultValue("false") boolean includeTransactions,
        @Parameter(description = "Include phased transactions detail info" )
            @QueryParam("includeExecutedPhased") @DefaultValue("false") boolean includeExecutedPhased
    ) {
        return getBlockResponse(blockId, height, timestamp, includeTransactions, includeExecutedPhased);
    }

    @Path("/one")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns Block information",
        description = "The API returns Block information with transaction info depending on specified params",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockDTO.class)))
        })
    @PermitAll
    public Response getBlockPost(
        @Parameter(description = "Block id (optional, default is last block)",
            schema = @Schema(implementation = Long.class, description="Block id (optional, default is last block"))
            @QueryParam("block") LongParameter blockId,
        @Parameter(description = "The block height (optional, default is last block).")
            @QueryParam("height") @DefaultValue("-1") @ValidBlockchainHeight int height,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp,
        @Parameter(description = "Include transactions detail info" )
            @QueryParam("includeTransactions") @DefaultValue("false") boolean includeTransactions,
        @Parameter(description = "Include phased transactions detail info" )
            @QueryParam("includeExecutedPhased") @DefaultValue("false") boolean includeExecutedPhased
    ) {
        return getBlockResponse(blockId, height, timestamp, includeTransactions, includeExecutedPhased);
    }

    private Response getBlockResponse(LongParameter blockId, int height, int timestamp,
                                      boolean includeTransactions, boolean includeExecutedPhased) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getBlock : \t blockId={}, height={}, timestamp={}, includeTransactions={}, includeExecutedPhased={}",
            blockId, height, timestamp, includeTransactions, includeExecutedPhased);
        Block blockData;
        if (blockId != null && blockId.get() != 0) {
            blockData = blockchain.getBlock(blockId.get());
        } else if (height > 0) {
            if (height > blockchain.getHeight()) {
                String errorMessage = String.format("Requested height in bigger when actual height: %s", height);
                log.warn(errorMessage);
                return response.error(ApiErrors.INCORRECT_PARAM, "height", height).build();
            }
            blockData = blockchain.getBlockAtHeight(height);
        } else if (timestamp > 0) {
            blockData = blockchain.getLastBlock(timestamp);
        } else {
            blockData = blockchain.getLastBlock();
        }
        if (blockData == null) {
            return response.error(ApiErrors.UNKNOWN_VALUE, "block", blockData).build();
        }
        if (includeTransactions) {
            blockchain.getOrLoadTransactions(blockData);
        }
        blockConverter.setAddTransactions(includeTransactions);
        blockConverter.setAddPhasedTransactions(includeExecutedPhased);
        BlockDTO dto = blockConverter.convert(blockData);
        if (!includeTransactions) {
            long count = blockchain.getBlockTransactionCount(blockData.getId());
            dto.setNumberOfTransactions(count);
        }
        log.trace("getBlock result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/id")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns Block ID by height",
        description = "The API returns Block ID only by specified height",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockDTO.class))) // ONLY ONE field is returned actually !!
        })
    @PermitAll
    public Response getBlockId(
        @Parameter(description = "The block height, mandatory", required = true)
            @QueryParam("height") @ValidBlockchainHeight int height
    ) {
        return getBlockByIdResponse(height);
    }

    @Path("/id")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns Block ID by height",
        description = "The API returns Block ID only by specified height",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlockDTO.class))) // ONLY ONE field is returned actually !!
        })
    @PermitAll
    public Response getBlockIdPost(
        @Parameter(description = "The block height, mandatory", required = true)
            @QueryParam("height") @ValidBlockchainHeight int height
    ) {
        return getBlockByIdResponse(height);
    }

    private Response getBlockByIdResponse(int height) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getBlockId : \t height={}", height);
        Long blockId = null;
        if (height > 0) {
            if (height > blockchain.getHeight()) {
                String errorMessage = String.format("Requested height in bigger when actual height: %s", height);
                log.warn(errorMessage);
                return response.error(ApiErrors.INCORRECT_PARAM, "height", height).build();
            }
            blockId = blockchain.getBlockIdAtHeight(height);
        }
        if (blockId == null) {
            return response.error(ApiErrors.UNKNOWN_VALUE, "blockId by height", height).build();
        }
        BlockDTO dto = new BlockDTO();
        dto.setBlock(Long.toUnsignedString(blockId));
        log.trace("getBlockId result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns List of Block information using first/last indexes",
        description = "The API returns List of Block information with transaction info depending on specified params."
            + " first/last index specifies height limits, blocks are selected for timestamp bigger then specified 'timestamp'",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlocksResponse.class)))
        })
    @PermitAll
    public Response getBlocks(
        @Parameter(description = "A zero-based index to the first, last asset ID to retrieve (optional).",
            schema = @Schema(implementation = FirstLastIndexBeanParam.class))
            @BeanParam FirstLastIndexBeanParam indexBeanParam,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp,
        @Parameter(description = "Include transactions detail info" )
            @QueryParam("includeTransactions") @DefaultValue("false") boolean includeTransactions,
        @Parameter(description = "Include phased transactions detail info" )
            @QueryParam("includeExecutedPhased") @DefaultValue("false") boolean includeExecutedPhased

    ) {
        return getBlockListResponse(indexBeanParam, timestamp, includeTransactions, includeExecutedPhased);
    }

    @Path("/list")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns List of Block information using first/last indexes",
        description = "The API returns List of Block information with transaction info depending on specified params."
            + " first/last index specifies height limits, blocks are selected for timestamp bigger then specified 'timestamp'",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlocksResponse.class)))
        })
    @PermitAll
    public Response getBlocksPost(
        @Parameter(description = "A zero-based index to the first, last asset ID to retrieve (optional).",
            schema = @Schema(implementation = FirstLastIndexBeanParam.class))
        @BeanParam FirstLastIndexBeanParam indexBeanParam,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp,
        @Parameter(description = "Include transactions detail info" )
            @QueryParam("includeTransactions") @DefaultValue("false") boolean includeTransactions,
        @Parameter(description = "Include phased transactions detail info" )
            @QueryParam("includeExecutedPhased") @DefaultValue("false") boolean includeExecutedPhased

    ) {
        return getBlockListResponse(indexBeanParam, timestamp, includeTransactions, includeExecutedPhased);
    }

    private Response getBlockListResponse(FirstLastIndexBeanParam indexBeanParam, int timestamp,
                                          boolean includeTransactions, boolean includeExecutedPhased) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getBlocks : \t indexBeanParam={}, timestamp={}, includeTransactions={}, includeExecutedPhased={}",
            indexBeanParam, timestamp, includeTransactions, includeExecutedPhased);
        BlocksResponse dto = new BlocksResponse();
        List<BlockDTO> blockDataList;
        Block lastBlock = blockchain.getLastBlock();
        if (lastBlock != null) {
            indexBeanParam.adjustIndexes(maxAPIFetchRecords);
            blockConverter.setAddTransactions(includeTransactions);
            blockConverter.setAddPhasedTransactions(includeExecutedPhased);
            List<Block> result = blockchain.getBlocksFromShards(indexBeanParam.getFirstIndex(), indexBeanParam.getLastIndex(), timestamp);
            log.trace("getBlocks result [{}]: \t indexBeanParam={}, timestamp={}, includeTransactions={}, includeExecutedPhased={}",
                result.size(), indexBeanParam, timestamp, includeTransactions, includeExecutedPhased);
            blockDataList = blockConverter.convert(result);
            dto.setBlocks(blockDataList);
        } else {
            log.warn("There are no blocks in db...");
            dto.setBlocks(Collections.emptyList());
        }
        log.trace("getBlocks result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/ec")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns ECBlock info by timestamp",
        description = "The API returns ECBlockDTO by specified timestamp",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ECBlockDTO.class)))
        })
    @PermitAll
    public Response getECBlock(
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp
    ) {
        return getBlockByEcResponse(timestamp);
    }

    @Path("/ec")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns ECBlock info by timestamp",
        description = "The API returns ECBlockDTO by specified timestamp",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ECBlockDTO.class)))
        })
    @PermitAll
    public Response getECBlockPost(
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp
    ) {
        return getBlockByEcResponse(timestamp);
    }

    private Response getBlockByEcResponse(int timestamp) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getECBlock : \t timestamp={}", timestamp);
        if (timestamp <= 0) {
            timestamp = timeService.getEpochTime();
        }
        EcBlockData ecBlock = blockchain.getECBlock(timestamp);

        if (ecBlock == null) {
            return response.error(ApiErrors.UNKNOWN_VALUE, "blockId by timestamp", timestamp).build();
        }
        ECBlockDTO dto = new ECBlockDTO();
        dto.setId(ecBlock.getId());
        dto.setEcBlockId(String.valueOf(ecBlock.getId()));
        dto.setEcBlockId(Long.toUnsignedString(ecBlock.getId()));
        dto.setEcBlockHeight(ecBlock.getHeight());
        dto.setTimestamp(timestamp);
        log.trace("getECBlock result: {}", dto);
        return response.bind(dto).build();
    }

}
