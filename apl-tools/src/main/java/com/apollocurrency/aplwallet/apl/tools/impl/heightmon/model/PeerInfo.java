/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

public class PeerInfo {
    private String host;
    private String schema;
    private Integer port;
    private static final String DEFAULT_SCHEMA = "http";

    public PeerInfo(String host) {
        this(host, DEFAULT_SCHEMA, null);
    }

    public PeerInfo(String host, String schema, Integer port) {
        this.host = host;
        this.schema = schema;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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
}
