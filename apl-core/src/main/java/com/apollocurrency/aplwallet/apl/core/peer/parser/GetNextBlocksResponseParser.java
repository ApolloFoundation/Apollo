/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.respons.GetNextBlocksResponse;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockParser;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class GetNextBlocksResponseParser implements JsonReqRespParser<GetNextBlocksResponse> {
    private final BlockParser blockParser;
    private final BlockchainConfig blockchainConfig;

    private final static int MAX_BLOCKS = 36;

    @Inject
    public GetNextBlocksResponseParser(BlockParser blockParser, BlockchainConfig blockchainConfig) {
        this.blockParser = blockParser;
        this.blockchainConfig = blockchainConfig;
    }

    @Override
    public GetNextBlocksResponse parse(JSONObject json) {
        List<BlockImpl> blockList = new ArrayList<>();
        //
        // Get the list of blocks.  We will stop parsing blocks if we encounter
        // an invalid block.  We will return the valid blocks and reset the stop
        // index so no more blocks will be processed.
        //
        List<JSONObject> nextBlocks = (List<JSONObject>) json.get("nextBlocks");
        if (nextBlocks == null) {
            return null;
        }

        if (nextBlocks.size() > MAX_BLOCKS) {
            GetNextBlocksResponse nextBlocksResponse = new GetNextBlocksResponse(blockList);
            nextBlocksResponse.setErrorCode(1);
            nextBlocksResponse.setCause("Too many nextBlocks");
            return nextBlocksResponse;
        }

        try {
            for (JSONObject blockData : nextBlocks) {
                BlockImpl parsedBlock = blockParser.parseBlock(blockData, blockchainConfig.getCurrentConfig().getInitialBaseTarget());
                parsedBlock.getTransactions().forEach(Transaction::resetFail); // error messages should be obtained node independently
                blockList.add(parsedBlock);
            }
        } catch (AplException.NotValidException | RuntimeException e) {
            log.debug("Failed to parse block(s): " + e.toString(), e);
            GetNextBlocksResponse nextBlocksResponse = new GetNextBlocksResponse(blockList);
            nextBlocksResponse.setErrorCode(1);
            nextBlocksResponse.setCause("Failed to parse block(s): " + e.toString());
            return nextBlocksResponse;
        }
        return new GetNextBlocksResponse(blockList);
    }

}
