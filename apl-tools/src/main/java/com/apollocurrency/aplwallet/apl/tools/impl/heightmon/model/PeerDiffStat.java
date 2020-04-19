/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import com.apollocurrency.aplwallet.apl.util.Version;

import java.util.Objects;

public class PeerDiffStat {
    private int diff1;
    private int diff2;
    private String peer1;
    private String peer2;
    private int milestoneHeight;
    private int height1;
    private int height2;
    private Version version1;
    private Version version2;
    private String shard1;
    private String shard2;
    private String shardStatus;

    public PeerDiffStat() {
    }

    public PeerDiffStat(int diff1, int diff2, String peer1, String peer2, int milestoneHeight, int height1, int height2, Version version1, Version version2, String shard1, String shard2, String shardStatus) {
        this.diff1 = diff1;
        this.diff2 = diff2;
        this.peer1 = peer1;
        this.peer2 = peer2;
        this.milestoneHeight = milestoneHeight;
        this.height1 = height1;
        this.height2 = height2;
        this.version1 = version1;
        this.version2 = version2;
        this.shard1 = shard1;
        this.shard2 = shard2;
        this.shardStatus = shardStatus;
    }

    public String getVersion1() {
        return version1.toString();
    }

    public void setVersion1(Version version1) {
        this.version1 = version1;
    }

    public String getVersion2() {
        return version2.toString();
    }

    public void setVersion2(Version version2) {
        this.version2 = version2;
    }

    public int getDiff1() {
        return diff1;
    }

    public void setDiff1(int diff1) {
        this.diff1 = diff1;
    }

    public int getDiff2() {
        return diff2;
    }

    public void setDiff2(int diff2) {
        this.diff2 = diff2;
    }

    public String getPeer1() {
        return peer1;
    }

    public void setPeer1(String peer1) {
        this.peer1 = peer1;
    }

    public String getPeer2() {
        return peer2;
    }

    public void setPeer2(String peer2) {
        this.peer2 = peer2;
    }

    public int getHeight1() {
        return height1;
    }

    public void setHeight1(int height1) {
        this.height1 = height1;
    }

    public int getHeight2() {
        return height2;
    }

    public void setHeight2(int height2) {
        this.height2 = height2;
    }

    public int getMilestoneHeight() {
        return milestoneHeight;
    }

    public void setMilestoneHeight(int milestoneHeight) {
        this.milestoneHeight = milestoneHeight;
    }

    public String getShard1() {
        return shard1;
    }

    public void setShard1(String shard1) {
        this.shard1 = shard1;
    }

    public String getShard2() {
        return shard2;
    }

    public void setShard2(String shard2) {
        this.shard2 = shard2;
    }

    public String getShardStatus() {
        return shardStatus;
    }

    public void setShardStatus(String shardStatus) {
        this.shardStatus = shardStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeerDiffStat)) return false;
        PeerDiffStat that = (PeerDiffStat) o;
        return diff1 == that.diff1 &&
            diff2 == that.diff2 &&
            milestoneHeight == that.milestoneHeight &&
            height1 == that.height1 &&
            height2 == that.height2 &&
            Objects.equals(peer1, that.peer1) &&
            Objects.equals(peer2, that.peer2) &&
            Objects.equals(version1, that.version1) &&
            Objects.equals(version2, that.version2) &&
            Objects.equals(shard1, that.shard1) &&
            Objects.equals(shard2, that.shard2) &&
            Objects.equals(shardStatus, that.shardStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diff1, diff2, peer1, peer2, milestoneHeight, height1, height2, version1, version2, shard1, shard2, shardStatus);
    }
}
