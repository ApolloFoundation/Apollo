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
    private int currentMaxDiff;
    private Map<Integer, Integer> diffForTime;

    public NetworkStats(List<PeerDiffStat> peerDiffStats, int currentMaxDiff, Map<Integer, Integer> diffForTime, Map<String, Integer> peerHeight) {
        this.peerDiffStats = peerDiffStats;
        this.currentMaxDiff = currentMaxDiff;
        this.diffForTime = diffForTime;
        this.peerHeight = peerHeight;
    }

    public NetworkStats() {
        this(new ArrayList<>(), -1, new LinkedHashMap<>(), new HashMap<>());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkStats)) return false;
        NetworkStats that = (NetworkStats) o;
        return currentMaxDiff == that.currentMaxDiff &&
                Objects.equals(peerDiffStats, that.peerDiffStats) &&
                Objects.equals(diffForTime, that.diffForTime) &&
                Objects.equals(peerHeight, that.peerHeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerDiffStats, currentMaxDiff, diffForTime, peerHeight);
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
