/*
 * Copyright © 2020-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * @author alukin@gmail.com
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
public class BaseP2PRequest implements Cloneable {

    public final Integer protocol = 1;
    @NotBlank
    public String requestType;
    @NotNull
    private UUID chainId;

    public BaseP2PRequest(String requestType, UUID chainId) {
        this.requestType = requestType;
        this.chainId = chainId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BaseP2PRequest.class.getSimpleName() + "[", "]")
            .add("protocol=" + protocol)
            .add("requestType='" + requestType + "'")
            .add("chainId=" + chainId)
            .toString();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
