/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.respons;

import com.apollocurrency.aplwallet.api.p2p.respons.BaseP2PResponse;
import com.apollocurrency.aplwallet.apl.core.model.BlockImpl;
import lombok.Getter;

import java.util.List;

//TODO finish https://firstb.atlassian.net/browse/APL-1629. Move to the apl-api module.
@Getter
public class GetNextBlocksResponse extends BaseP2PResponse {
    private final List<BlockImpl> nextBlocks;

    public GetNextBlocksResponse(List<BlockImpl> nextBlocks) {
        this.nextBlocks = nextBlocks;
    }

    public GetNextBlocksResponse(List<BlockImpl> nextBlocks, Exception exception) {
        this.nextBlocks = nextBlocks;
    }
}
