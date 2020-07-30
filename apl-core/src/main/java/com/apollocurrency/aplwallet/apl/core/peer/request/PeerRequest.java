/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.request;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.UUID;

public abstract class PeerRequest {

    private final String requestType;
    private final UUID chainId;
    private final Integer protocol = 1;

    public PeerRequest(String requestType, UUID chainId) {
        this.requestType = requestType;
        this.chainId = chainId;
    }

    public abstract String toJson() throws JsonProcessingException;

    public abstract String toBinary();

    public String getRequestType() {
        return requestType;
    }

    public UUID getChainId() {
        return chainId;
    }

    public Integer getProtocol() {
        return protocol;
    }
}
