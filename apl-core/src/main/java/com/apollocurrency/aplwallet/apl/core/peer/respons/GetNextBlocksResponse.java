/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.respons;

import com.apollocurrency.aplwallet.api.p2p.response.BaseP2PResponse;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import lombok.Getter;

import java.util.List;

//TODO finish https://firstb.atlassian.net/browse/APL-1629. Move to the apl-api module.
@Getter
public class GetNextBlocksResponse extends BaseP2PResponse {
    private final List<Block> nextBlocks;

    public GetNextBlocksResponse(List<Block> nextBlocks) {
        this.nextBlocks = nextBlocks;
    }

    public GetNextBlocksResponse(List<Block> nextBlocks, Exception exception) {
        this.nextBlocks = nextBlocks;
    }
}
