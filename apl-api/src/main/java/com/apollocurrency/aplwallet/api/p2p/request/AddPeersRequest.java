/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddPeersRequest extends BaseP2PRequest {
    private static final String requestType = "addPeers";

    private List<String> myPeers;
    private List<String> myServices;

    public AddPeersRequest(List<String> myPeers, List<String> myServices, UUID chainId) {
        super(requestType, chainId);
        this.myPeers = myPeers;
        this.myServices = myServices;
    }

}
