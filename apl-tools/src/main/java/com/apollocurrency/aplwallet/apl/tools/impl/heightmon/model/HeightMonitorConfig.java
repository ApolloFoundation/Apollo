/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeightMonitorConfig that = (HeightMonitorConfig) o;
        return Objects.equals(peersConfig, that.peersConfig) &&
                Objects.equals(maxBlocksDiffPeriods, that.maxBlocksDiffPeriods) &&
                Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peersConfig, maxBlocksDiffPeriods, port);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("HeightMonitorConfig{");
        sb.append("peersConfig=").append(peersConfig);
        sb.append(", maxBlocksDiffPeriods=").append(maxBlocksDiffPeriods);
        sb.append(", port=").append(port);
        sb.append('}');
        return sb.toString();
    }
}
