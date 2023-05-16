/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import lombok.Getter;

import java.util.StringJoiner;
import java.util.UUID;

@Getter
public class GetNextBlockIdsRequest extends BaseP2PRequest {
    private static final String REQUEST_TYPE = "getNextBlockIds";

    private final String blockId;
    private final int limit;


    public GetNextBlockIdsRequest(String blockId, int limit, UUID chainId) {
        super(REQUEST_TYPE, chainId);
        this.blockId = blockId;
        this.limit = limit;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GetNextBlockIdsRequest.class.getSimpleName() + "[", "]")
            .add("blockId='" + blockId + "'")
            .add("limit=" + limit)
            .add("super=" + super.toString())
            .toString();
    }
}
