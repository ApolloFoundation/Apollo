/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.request;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;

import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetCumulativeDifficultyRequest extends PeerRequest {
    private static final String requestType = "getCumulativeDifficulty";

    public GetCumulativeDifficultyRequest(UUID chainId) {
        super(requestType, chainId);
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
