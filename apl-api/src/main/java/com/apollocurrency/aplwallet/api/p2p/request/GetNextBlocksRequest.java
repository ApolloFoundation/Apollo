/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import lombok.Getter;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

@Getter
public class GetNextBlocksRequest extends BaseP2PRequest {
    private static final String REQUEST_TYPE = "getNextBlocks";

    private final List<String> blockIds;
    private final String blockId;

    public GetNextBlocksRequest(List<String> blockIds, String blockId, UUID chainId) {
        super(REQUEST_TYPE, chainId);
        this.blockIds = blockIds;
        this.blockId = blockId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GetNextBlocksRequest.class.getSimpleName() + "[", "]")
            .add("blockIds=[" + String.join(",", blockIds) + "]")
            .add("blockId='" + blockId + "'")
            .add("super=" + super.toString())
            .toString();
    }
}
