/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callable method to get the next block segment from the selected peer
 */
public class GetNextBlocks implements Callable<List<BlockImpl>> {
    private static final Logger log = LoggerFactory.getLogger(GetNextBlocks.class);
    
    private BlockchainConfig blockchainConfig;   
    /** Callable future */
    private Future<List<BlockImpl>> future;
    /** Peer */
    private Peer peer;
    /** Block identifier list */
    private final List<Long> blockIds;
    /** Start index */
    private int start;
    /** Stop index */
    private int stop;
    /** Request count */
    private int requestCount;
    /** Time it took to return getNextBlocks */
    private long responseTime;
    /**
     * height of the block from which we will start to download next blocks
     */
    private int startHeight;

    /**
     * Create the callable future
     *
     * @param   blockIds            Block identifier list
     * @param   start               Start index within the list
     * @param   stop                Stop index within the list
     * @param   startHeight         Height of the block from which we will start to download blockchain
     */
    public GetNextBlocks(List<Long> blockIds, int start, int stop, int startHeight, BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
        this.blockIds = blockIds;
        this.start = start;
        this.stop = stop;
        this.startHeight = startHeight;
        this.requestCount = 0;
    }

    /**
     * Return the result
     *
     * @return                      List of blocks or null if an error occurred
     */
    @Override
    public List<BlockImpl> call() {
        requestCount++;
        //
        // Build the block request list
        //
        JSONArray idList = new JSONArray();
        for (int i = start + 1; i <= stop; i++) {
            idList.add(Long.toUnsignedString(blockIds.get(i)));
        }
        JSONObject request = new JSONObject();
        request.put("requestType", "getNextBlocks");
        request.put("blockIds", idList);
        request.put("blockId", Long.toUnsignedString(blockIds.get(start)));
        request.put("chainId", blockchainConfig.getChain().getChainId());
        long startTime = System.currentTimeMillis();
        JSONObject response = peer.send(JSON.prepareRequest(request), blockchainConfig.getChain().getChainId(), 10 * 1024 * 1024, false);
        responseTime = System.currentTimeMillis() - startTime;
        if (response == null) {
            return null;
        }
        //
        // Get the list of blocks.  We will stop parsing blocks if we encounter
        // an invalid block.  We will return the valid blocks and reset the stop
        // index so no more blocks will be processed.
        //
        List<JSONObject> nextBlocks = (List<JSONObject>) response.get("nextBlocks");
        if (nextBlocks == null) {
            return null;
        }
        if (nextBlocks.size() > 36) {
            log.debug("Obsolete or rogue peer " + peer.getHost() + " sends too many nextBlocks, blacklisting");
            peer.blacklist("Too many nextBlocks");
            return null;
        }
        List<BlockImpl> blockList = new ArrayList<>(nextBlocks.size());
        try {
            int count = stop - start;
            for (JSONObject blockData : nextBlocks) {
                blockList.add(BlockImpl.parseBlock(blockData));
                if (--count <= 0) {
                    break;
                }
            }
        } catch (RuntimeException | AplException.NotValidException e) {
            log.debug("Failed to parse block: " + e.toString(), e);
            peer.blacklist(e);
            stop = start + blockList.size();
        }
        return blockList;
    }

    /**
     * Return the callable future
     *
     * @return                      Callable future
     */
    public Future<List<BlockImpl>> getFuture() {
        return future;
    }

    /**
     * Set the callable future
     *
     * @param   future              Callable future
     */
    public void setFuture(Future<List<BlockImpl>> future) {
        this.future = future;
    }

    /**
     * Return the peer
     *
     * @return                      Peer
     */
    public Peer getPeer() {
        return peer;
    }

    /**
     * Set the peer
     *
     * @param   peer                Peer
     */
    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    /**
     * Return the start index
     *
     * @return                      Start index
     */
    public int getStart() {
        return start;
    }

    /**
     * Set the start index
     *
     * @param   start               Start index
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Return the stop index
     *
     * @return                      Stop index
     */
    public int getStop() {
        return stop;
    }

    /**
     * Return the request count
     *
     * @return                      Request count
     */
    public int getRequestCount() {
        return requestCount;
    }

    /**
     * Return the response time
     *
     * @return                      Response time
     */
    public long getResponseTime() {
        return responseTime;
    }
    
}
