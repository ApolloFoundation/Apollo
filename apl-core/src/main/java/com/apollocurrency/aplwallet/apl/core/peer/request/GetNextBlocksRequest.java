/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.request;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.UUID;

@Getter
public class GetNextBlocksRequest extends PeerRequest {
    private static final String requestType = "getNextBlocks";

    private final List<String> blockIds;
    private final String blockId;

    public GetNextBlocksRequest(List<String> blockIds, String blockId, UUID chainId) {
        super(requestType, chainId);
        this.blockIds = blockIds;
        this.blockId = blockId;
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
