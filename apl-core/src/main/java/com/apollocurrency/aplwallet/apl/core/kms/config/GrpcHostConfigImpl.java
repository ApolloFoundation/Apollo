package com.apollocurrency.aplwallet.apl.core.kms.config;

import io.firstbridge.kms.client.grpx.config.GrpcHostConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * gRPC client local configuration class
 */
@Setter
@Getter
@ToString
public class GrpcHostConfigImpl implements GrpcHostConfig {

    //(name = "quarkus.grpc.clients.health.url")
    private String url;

    public GrpcHostConfigImpl(String url) {
        this.url = url;
    }
}