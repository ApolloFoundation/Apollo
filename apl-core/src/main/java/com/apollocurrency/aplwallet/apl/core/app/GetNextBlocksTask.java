/*
 * Copyright © 2019-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.p2p.request.GetNextBlocksRequest;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetNextBlocksResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.respons.GetNextBlocksResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Callable method to get the next block segment from the selected peer
 */
@Slf4j
public class GetNextBlocksTask implements Callable<List<BlockImpl>> {
    /**
     * Block identifier list
     */
    private final List<Long> blockIds;
    private final BlockchainConfig blockchainConfig;
    private final GetNextBlocksResponseParser getNextBlocksResponseParser;
    /**
     * Callable future
     */
    private Future<List<BlockImpl>> future;
    /**
     * Peer
     */
    private Peer peer;
    /**
     * Start index
     */
    private int start;
    /**
     * Stop index
     */
    private int stop;
    /**
     * Request count
     */
    private int requestCount;
    /**
     * Time it took to return getNextBlocks
     */
    private long responseTime;
    /**
     * height of the block from which we will start to download next blocks
     */
    private int startHeight;

    /**
     * Create the callable future
     *
     * @param blockIds    Block identifier list
     * @param start       Start index within the list
     * @param stop        Stop index within the list
     * @param startHeight Height of the block from which we will start to download blockchain
     */
    public GetNextBlocksTask(List<Long> blockIds, int start, int stop, int startHeight,
                             BlockchainConfig blockchainConfig,
                             GetNextBlocksResponseParser getNextBlocksResponseParser) {
        this.blockchainConfig = blockchainConfig;
        this.getNextBlocksResponseParser = getNextBlocksResponseParser;
        this.blockIds = blockIds;
        this.start = start;
        this.stop = stop;
        this.startHeight = startHeight;
        this.requestCount = 0;
    }

    /**
     * Return the result
     *
     * @return List of blocks or null if an error occurred
     */
    @Override
    public List<BlockImpl> call() {
        requestCount++;
        //
        // Build the block request list
        //
        List<String> idList = new ArrayList<>();
        for (int i = start + 1; i <= stop; i++) {
            idList.add(Long.toUnsignedString(blockIds.get(i)));
        }

        GetNextBlocksRequest request = new GetNextBlocksRequest(
            idList,
            Long.toUnsignedString(blockIds.get(start)),
            blockchainConfig.getChain().getChainId()
        );

        GetNextBlocksResponse response;
        long startTime = System.currentTimeMillis();
        try {
            response = peer.send(request, getNextBlocksResponseParser);
        } catch (PeerNotConnectedException ex) {
            return null;
        } finally {
            responseTime = System.currentTimeMillis() - startTime;
        }

        if (response == null) {
            log.debug("NULL GetNextBlocks response from peer: {}", peer.getAnnouncedAddress());
            return null;
        }

        if (response.getErrorCode() != 0) {
            log.debug("Failed to parse block(s) from {} cause: {}", peer.getAnnouncedAddress(), response.getCause());
            peer.blacklist(response.getCause());
            stop = start + response.getNextBlocks().size();
        }

        int count = stop - start;
        if (response.getNextBlocks().size() > count) {
            return response.getNextBlocks().subList(0, count);
        } else {
            return response.getNextBlocks();
        }
    }

    /**
     * Return the callable future
     *
     * @return Callable future
     */
    public Future<List<BlockImpl>> getFuture() {
        return future;
    }

    /**
     * Set the callable future
     *
     * @param future Callable future
     */
    public void setFuture(Future<List<BlockImpl>> future) {
        this.future = future;
    }

    /**
     * Return the peer
     *
     * @return Peer
     */
    public Peer getPeer() {
        return peer;
    }

    /**
     * Set the peer
     *
     * @param peer Peer
     */
    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    /**
     * Return the start index
     *
     * @return Start index
     */
    public int getStart() {
        return start;
    }

    /**
     * Set the start index
     *
     * @param start Start index
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Return the stop index
     *
     * @return Stop index
     */
    public int getStop() {
        return stop;
    }

    /**
     * Return the request count
     *
     * @return Request count
     */
    public int getRequestCount() {
        return requestCount;
    }

    /**
     * Return the response time
     *
     * @return Response time
     */
    public long getResponseTime() {
        return responseTime;
    }

}
