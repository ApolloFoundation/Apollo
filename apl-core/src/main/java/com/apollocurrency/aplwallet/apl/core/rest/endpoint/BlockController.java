/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.response.BlocksResponse;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.LongParameter;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidBlockchainHeight;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidTimestamp;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
@OpenAPIDefinition(info = @Info(description = "Provide several block retrieving methods"))
@Singleton
@Path("/block")
public class BlockController {
    private Blockchain blockchain;
    private BlockConverter blockConverter;
    private FirstLastIndexParser indexParser;
    private TimeService timeService;

    @Inject
    public BlockController(Blockchain blockchain, BlockConverter blockConverter,
                           FirstLastIndexParser indexParser, TimeService timeService) {
        this.blockchain = Objects.requireNonNull(blockchain);
        this.blockConverter = Objects.requireNonNull(blockConverter);
        this.indexParser = Objects.requireNonNull(indexParser);
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

    private Response getBlockResponse(@QueryParam("block") @Parameter(description = "Block id (optional, default is last block)", schema = @Schema(implementation = Long.class, description = "Block id (optional, default is last block")) LongParameter blockId, @ValidBlockchainHeight @DefaultValue("-1") @QueryParam("height") @Parameter(description = "The block height (optional, default is last block).") int height, @ValidTimestamp @DefaultValue("-1") @QueryParam("timestamp") @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional).") int timestamp, @DefaultValue("false") @QueryParam("includeTransactions") @Parameter(description = "Include transactions detail info") boolean includeTransactions, @DefaultValue("false") @QueryParam("includeExecutedPhased") @Parameter(description = "Include phased transactions detail info") boolean includeExecutedPhased) {
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
        blockConverter.setAddTransactions(includeTransactions);
        blockConverter.setAddPhasedTransactions(includeExecutedPhased);
        BlockDTO dto = blockConverter.convert(blockData);
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

    private Response getBlockByIdResponse(@ValidBlockchainHeight @QueryParam("height") @Parameter(description = "The block height, mandatory", required = true) int height) {
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
        dto.setBlock(blockId.toString());
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
        @Parameter(description = "A zero-based index to the 'first height' to retrieve (optional)." )
            @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the 'last height' to retrieve (optional)." )
            @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp,
        @Parameter(description = "Include transactions detail info" )
            @QueryParam("includeTransactions") @DefaultValue("false") boolean includeTransactions,
        @Parameter(description = "Include phased transactions detail info" )
            @QueryParam("includeExecutedPhased") @DefaultValue("false") boolean includeExecutedPhased

    ) {
        return getBlockListResponse(firstIndex, lastIndex, timestamp, includeTransactions, includeExecutedPhased);
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
        @Parameter(description = "A zero-based index to the 'first height' to retrieve (optional)." )
            @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the 'last height' to retrieve (optional)." )
            @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp,
        @Parameter(description = "Include transactions detail info" )
            @QueryParam("includeTransactions") @DefaultValue("false") boolean includeTransactions,
        @Parameter(description = "Include phased transactions detail info" )
            @QueryParam("includeExecutedPhased") @DefaultValue("false") boolean includeExecutedPhased

    ) {
        return getBlockListResponse(firstIndex, lastIndex, timestamp, includeTransactions, includeExecutedPhased);
    }

    private Response getBlockListResponse(@DefaultValue("0") @QueryParam("firstIndex") @Parameter(description = "A zero-based index to the 'first height' to retrieve (optional).") @PositiveOrZero int firstIndex, @DefaultValue("-1") @QueryParam("lastIndex") @Parameter(description = "A zero-based index to the 'last height' to retrieve (optional).") int lastIndex, @ValidTimestamp @DefaultValue("-1") @QueryParam("timestamp") @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional).") int timestamp, @DefaultValue("false") @QueryParam("includeTransactions") @Parameter(description = "Include transactions detail info") boolean includeTransactions, @DefaultValue("false") @QueryParam("includeExecutedPhased") @Parameter(description = "Include phased transactions detail info") boolean includeExecutedPhased) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getBlocks : \t firstIndex={}, lastIndex={}, timestamp={}, includeTransactions={}, includeExecutedPhased={}",
            firstIndex, lastIndex, timestamp, includeTransactions, includeExecutedPhased);
        BlocksResponse dto = new BlocksResponse();
        List<BlockDTO> blockDataList;
        Block lastBlock = blockchain.getLastBlock();
        if (lastBlock != null) {
            FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);
            blockConverter.setAddTransactions(includeTransactions);
            blockConverter.setAddPhasedTransactions(includeExecutedPhased);
            Stream<Block> steam = blockchain.getBlocksStream(flIndex.getFirstIndex(), flIndex.getLastIndex(), timestamp);
            List<Block> result = steam.collect(Collectors.toList());
            log.trace("getBlocks result [{}]: \t firstIndex={}, lastIndex={}, timestamp={}, includeTransactions={}, includeExecutedPhased={}",
                result.size(), flIndex.getFirstIndex(), flIndex.getLastIndex(), timestamp, includeTransactions, includeExecutedPhased);
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

    private Response getBlockByEcResponse(@ValidTimestamp @DefaultValue("-1") @QueryParam("timestamp") @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional).") int timestamp) {
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
