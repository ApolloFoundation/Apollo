/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PeersConfig {
    private static final int DEFAULT_PORT = 7876;
    private List<PeerInfo> peersInfo;
    private int defaultPort;

    public PeersConfig(List<PeerInfo> peersInfo, int defaultPort) {
        this.peersInfo = peersInfo;
        this.defaultPort = defaultPort;
    }

    @JsonCreator
    public PeersConfig(@JsonProperty("peersInfo") List<PeerInfo> peersInfo) {
        this(peersInfo, DEFAULT_PORT);
    }

    public List<PeerInfo> getPeersInfo() {
        return peersInfo;
    }

    public void setPeersInfo(List<PeerInfo> peersInfo) {
        this.peersInfo = peersInfo;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public void setDefaultPort(int defaultPort) {
        this.defaultPort = defaultPort;
    }
}
