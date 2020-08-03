/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import lombok.Getter;

import java.util.UUID;

@Getter
public class GetNextBlockIdsRequest extends BaseP2PRequest {
    private static final String requestType = "getNextBlockIds";

    private final String blockId;
    private final int limit;


    public GetNextBlockIdsRequest(String blockId, int limit, UUID chainId) {
        super(requestType, chainId);
        this.blockId = blockId;
        this.limit = limit;
    }

}
