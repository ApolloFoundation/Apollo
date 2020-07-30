/*
 * Copyright © 2020-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * @author alukin@gmail.com
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseP2PRequest {

    public final Integer protocol = 1;
    public final String requestType;
    private final UUID chainId;

    public BaseP2PRequest(String requestType, UUID chainId) {
        this.requestType = requestType;
        this.chainId = chainId;
    }
}
