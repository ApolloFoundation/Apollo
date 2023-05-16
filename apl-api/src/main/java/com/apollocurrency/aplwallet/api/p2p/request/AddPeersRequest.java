/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddPeersRequest extends BaseP2PRequest {
    private static final String REQUEST_TYPE = "addPeers";

    private List<String> myPeers;
    private List<String> myServices;

    public AddPeersRequest(List<String> myPeers, List<String> myServices, UUID chainId) {
        super(REQUEST_TYPE, chainId);
        this.myPeers = myPeers;
        this.myServices = myServices;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AddPeersRequest.class.getSimpleName() + "[", "]")
            .add("myPeers=[" + String.join(",", myPeers) + "]")
            .add("myServices=[" + String.join(",", myServices) + "]")
            .add("super=" + super.toString())
            .toString();
    }
}
