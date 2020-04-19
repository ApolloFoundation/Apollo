/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.UnknownHostException;
import java.util.Objects;

public class PeerInfo {
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private String host;
    private String schema;
    private Integer port;

    @JsonCreator
    public PeerInfo(@JsonProperty("host") String host) {
        this(host, HTTP, null);
    }

    public PeerInfo(String host, String schema, Integer port) {
        this.host = host;
        this.schema = schema;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) throws UnknownHostException {
        this.host = host;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
        if (port == null) {
            if (HTTP.equalsIgnoreCase(schema)) {
                port = 80;
            } else if (HTTPS.equalsIgnoreCase(schema)) {
                port = 443;
            }
        }
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeerInfo)) return false;
        PeerInfo peerInfo = (PeerInfo) o;
        return Objects.equals(getHost(), peerInfo.getHost()) &&
            Objects.equals(schema, peerInfo.schema) &&
            Objects.equals(port, peerInfo.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost(), schema, port);
    }
}
