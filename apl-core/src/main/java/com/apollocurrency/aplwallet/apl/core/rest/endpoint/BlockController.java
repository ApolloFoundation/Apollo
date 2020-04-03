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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.response.BlocksResponse;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.LongParameter;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidBlockchainHeight;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidTimestamp;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
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

    @Inject
    public BlockController(Blockchain blockchain, BlockConverter blockConverter) {
        this.blockchain = Objects.requireNonNull(blockchain);
        this.blockConverter = Objects.requireNonNull(blockConverter);
    }

    @Path("/")
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
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getBlock : \t blockId = {}, height={}, timestamp={}, includeTransactions={}, includeExecutedPhased={}",
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

    @Path("/blocks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns List of Block information",
        description = "The API returns List of Block information with transaction info depending on specified params",
        tags = {"block"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlocksResponse.class)))
        })
    @PermitAll
    public Response getBlocks(
        @Parameter(description = "Block id (optional, default is last block)") @QueryParam("block") String blockIdStr,
        @Parameter(description = "The height of the blockchain to determine the currency count (optional, default is last block).")
            @QueryParam("height") @ValidBlockchainHeight int height,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." )
            @QueryParam("timestamp") @PositiveOrZero int timestamp,
        @Parameter(description = "Include transactions detail info" )
            @QueryParam("includeTransactions") @DefaultValue("false") boolean includeTransactions,
        @Parameter(description = "Include phased transactions detail info" )
            @QueryParam("includeExecutedPhased") @DefaultValue("false") boolean includeExecutedPhased

    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getBlock : \t 'block' = {}, height={}, timestamp={}, includeTransactions={}, includeExecutedPhased={}",
            blockIdStr, height, timestamp, includeTransactions, includeExecutedPhased);
        List<Block> blockDataList;
        Long blockId;
        BlocksResponse dto = new BlocksResponse();
        try {
            blockId = Convert.fullHashToId(Convert.parseHexString(blockIdStr));
            blockDataList = CollectionUtil.toList( blockchain.getBlocks(1, 10) );
        } catch (NumberFormatException e) {
            String errorMessage = String.format("Error converting block ID: %s, e = %s", blockIdStr, e.getMessage());
            log.warn(errorMessage, e);
            return response.error(ApiErrors.INCORRECT_PARAM, "blockId", blockIdStr).build();
        }
        dto.setBlocks(blockConverter.convert(blockDataList));
        log.trace("getBlock result: {}", dto);
        return response.bind(dto).build();
    }

}
