/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.blockchain;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Block returned by a peer
 */
@Getter
@AllArgsConstructor
public class PeerBlock {
    /**
     * Peer
     */
    private final Peer peer;
    /**
     * Block
     */
    private final Block block;
}
