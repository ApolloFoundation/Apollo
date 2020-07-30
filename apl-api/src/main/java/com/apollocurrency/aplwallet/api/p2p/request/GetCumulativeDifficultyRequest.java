/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetCumulativeDifficultyRequest extends BaseP2PRequest {
    private static final String requestType = "getCumulativeDifficulty";

    public GetCumulativeDifficultyRequest(UUID chainId) {
        super(requestType, chainId);
    }

}
