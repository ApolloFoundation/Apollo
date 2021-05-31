package com.apollocurrency.aplwallet.apl.core.kms.config;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

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

    private String host;

    private int grpcPort;

    private Optional<InetSocketAddress> inetSocketAddress = Optional.empty();

    public GrpcHostConfigImpl(
        String host,
        int grpcPort) {
        this.host = host;
        this.grpcPort = grpcPort;
    }

    @Override
    public Optional<InetSocketAddress> getSocketAddress() {
        if (inetSocketAddress.isEmpty()) {
            inetSocketAddress = validateKmsUrl();
        }
        return inetSocketAddress;
    }

    private Optional<InetSocketAddress> validateKmsUrl() {
        if (host == null || host.strip().isBlank()) return Optional.empty();
        try {
            // WORKAROUND: add any scheme to make the resulting URI valid.
            URI uri = new URI("dns",
                null,
                this.host,
                this.grpcPort,
                null,
                null,
                null ); // may throw URISyntaxException
            String host = uri.getHost();
            int port = uri.getPort();

            if (uri.getHost() == null || uri.getPort() == -1) {
                System.err.println("Can not assign KMS, URI must have host and port parts: " + this.host + ":" + this.grpcPort);
                return Optional.empty();
            }
            // validation succeeded
            inetSocketAddress = Optional.of(new InetSocketAddress(host, port));
            return inetSocketAddress;

        } catch (URISyntaxException ex) {
            // validation failed
            System.err.println("KMS ip/URI is not valid: " + this.host + ":" + this.grpcPort);
        }
        return Optional.empty();
    }

    @Override
    public String getHost() {
        return this.host;
    }
}