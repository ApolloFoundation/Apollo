/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetMilestoneBlockIdsRequest extends BaseP2PRequest {

    private static final String requestType = "getMilestoneBlockIds";

    private String lastBlockId;
    private String lastMilestoneBlockId;

    public GetMilestoneBlockIdsRequest(UUID chainId) {
        super(requestType, chainId);
    }

}
