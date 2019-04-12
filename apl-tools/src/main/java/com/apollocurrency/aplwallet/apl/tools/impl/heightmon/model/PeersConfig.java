/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;


import java.util.List;

public class PeersConfig {
    private List<PeerInfo> peersInfo;
    private int defaultPort;
    private static final int DEFAULT_PORT = 7876;

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

    public PeersConfig(List<PeerInfo> peersInfo, int defaultPort) {
        this.peersInfo = peersInfo;
        this.defaultPort = defaultPort;
    }

    public PeersConfig(List<PeerInfo> peersInfo) {
        this(peersInfo, DEFAULT_PORT);
    }
}
