/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.request;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;

import java.util.UUID;

@Getter
public class GetNextBlockIdsRequest extends PeerRequest {
    private static final String requestType = "getNextBlockIds";

    private final String blockId;
    private final int limit;


    public GetNextBlockIdsRequest(String blockId, int limit, UUID chainId) {
        super(requestType, chainId);
        this.blockId = blockId;
        this.limit = limit;
    }

    @Override
    public String toJson() throws JsonProcessingException {
        return JSON.getMapper().writeValueAsString(this);
    }

    @Override
    public String toBinary() {
        throw new NotImplementedException("Method 'toBinary' hasn't implemented yet.)");
    }
}
