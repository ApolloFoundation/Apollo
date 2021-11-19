/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.StringJoiner;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetMilestoneBlockIdsRequest extends BaseP2PRequest {

    private static final String REQUEST_TYPE = "getMilestoneBlockIds";

    private String lastBlockId;
    private String lastMilestoneBlockId;

    public GetMilestoneBlockIdsRequest(UUID chainId) {
        super(REQUEST_TYPE, chainId);
    }

    public GetMilestoneBlockIdsRequest(UUID chainId, String lastBlockId, String lastMilestoneBlockId) {
        this(chainId);
        this.lastBlockId = lastBlockId;
        this.lastMilestoneBlockId = lastMilestoneBlockId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GetMilestoneBlockIdsRequest.class.getSimpleName() + "[", "]")
            .add("lastBlockId='" + lastBlockId + "'")
            .add("lastMilestoneBlockId='" + lastMilestoneBlockId + "'")
            .add("super=" + super.toString())
            .toString();
    }
}
