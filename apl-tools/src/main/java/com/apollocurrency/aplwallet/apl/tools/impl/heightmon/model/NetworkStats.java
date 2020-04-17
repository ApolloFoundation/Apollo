/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NetworkStats {
    private Map<String, Integer> peerHeight;
    private List<PeerDiffStat> peerDiffStats;
    private Map<String, List<String>> peerShards;
    private int currentMaxDiff;
    private Map<Integer, Integer> diffForTime;

    public NetworkStats(List<PeerDiffStat> peerDiffStats, int currentMaxDiff, Map<Integer, Integer> diffForTime, Map<String, Integer> peerHeight, Map<String, List<String>> peerShards) {
        this.peerDiffStats = peerDiffStats;
        this.currentMaxDiff = currentMaxDiff;
        this.diffForTime = diffForTime;
        this.peerHeight = peerHeight;
        this.peerShards = peerShards;
    }

    public NetworkStats() {
        this(new ArrayList<>(), -1, new LinkedHashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public Map<String, List<String>> getPeerShards() {
        return peerShards;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkStats)) return false;
        NetworkStats that = (NetworkStats) o;
        return currentMaxDiff == that.currentMaxDiff &&
            Objects.equals(peerHeight, that.peerHeight) &&
            Objects.equals(peerDiffStats, that.peerDiffStats) &&
            Objects.equals(peerShards, that.peerShards) &&
            Objects.equals(diffForTime, that.diffForTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerHeight, peerDiffStats, peerShards, currentMaxDiff, diffForTime);
    }

    public Map<String, Integer> getPeerHeight() {
        return peerHeight;
    }

    public List<PeerDiffStat> getPeerDiffStats() {
        return peerDiffStats;
    }

    public void setPeerDiffStats(List<PeerDiffStat> peerDiffStats) {
        this.peerDiffStats = peerDiffStats;
    }

    public int getCurrentMaxDiff() {
        return currentMaxDiff;
    }

    public void setCurrentMaxDiff(int currentMaxDiff) {
        this.currentMaxDiff = currentMaxDiff;
    }

    public Map<Integer, Integer> getDiffForTime() {
        return diffForTime;
    }

    public void setDiffForTime(Map<Integer, Integer> diffForTime) {
        this.diffForTime = diffForTime;
    }
}
