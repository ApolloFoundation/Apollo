/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class GetNextBlocksRequest extends BaseP2PRequest {
    private static final String requestType = "getNextBlocks";

    private final List<String> blockIds;
    private final String blockId;

    public GetNextBlocksRequest(List<String> blockIds, String blockId, UUID chainId) {
        super(requestType, chainId);
        this.blockIds = blockIds;
        this.blockId = blockId;
    }

}
