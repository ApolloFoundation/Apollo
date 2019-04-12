/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class PeerInfo {
    private InetAddress address;
    private String schema;
    private Integer port;
    private static final String DEFAULT_SCHEMA = "http";

    public PeerInfo(String host) throws UnknownHostException {
        this(host, DEFAULT_SCHEMA, null);
    }

    public PeerInfo(String host, String schema, Integer port) throws UnknownHostException {
        this.address = InetAddress.getByName(host);
        this.schema = schema;
        this.port = port;
    }

    public String getHost() {
        return address.getHostName();
    }

    public void setHost(String host) throws UnknownHostException {
        this.address = InetAddress.getByName(host);
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
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
