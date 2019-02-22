/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;

/**
 * Block returned by a peer
 */
class PeerBlock {

    /** Peer */
    private final Peer peer;
    /** Block */
    private final Block block;

    /**
     * Create the peer block
     *
     * @param   peer                Peer
     * @param   block               Block
     */
    public PeerBlock(Peer peer, Block block) {
        this.peer = peer;
        this.block = block;
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
     * Return the block
     *
     * @return                      Block
     */
    public Block getBlock() {
        return block;
    }
    
}
