/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import java.util.List;

public class HeightMonitorConfig {
    private PeersConfig peersConfig;
    private List<Integer> maxBlocksDiffPeriods;
    private Integer port;

    public HeightMonitorConfig(PeersConfig config, List<Integer> maxBlocksDiffPeriods, Integer port) {
        this.peersConfig = config;
        this.maxBlocksDiffPeriods = maxBlocksDiffPeriods;
        this.port = port;
    }

    public HeightMonitorConfig() {
    }

    public PeersConfig getPeersConfig() {
        return peersConfig;
    }

    public void setPeersConfig(PeersConfig peersConfig) {
        this.peersConfig = peersConfig;
    }

    public List<Integer> getMaxBlocksDiffPeriods() {
        return maxBlocksDiffPeriods;
    }

    public void setMaxBlocksDiffPeriods(List<Integer> maxBlocksDiffPeriods) {
        this.maxBlocksDiffPeriods = maxBlocksDiffPeriods;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
