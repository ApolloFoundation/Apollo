/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.respons;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockImpl;
import lombok.Getter;

import java.util.List;

@Getter
public class GetNextBlocksResponse extends PeerResponse {
    private final List<BlockImpl> nextBlocks;

    public GetNextBlocksResponse(List<BlockImpl> nextBlocks) {
        this.nextBlocks = nextBlocks;
    }

    public GetNextBlocksResponse(List<BlockImpl> nextBlocks, Exception exception) {
        super(exception);
        this.nextBlocks = nextBlocks;
    }
}
