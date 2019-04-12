/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import java.util.Objects;

public class PeerDiffStat {
    private int diff1;
    private int diff2;
    private String peer1;
    private String peer2;
    private int height1;
    private int height2;

    public PeerDiffStat() {
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

    public PeerDiffStat(int diff1, int diff2, String peer1, String peer2, int height1, int height2) {
        this.diff1 = diff1;
        this.diff2 = diff2;
        this.peer1 = peer1;
        this.peer2 = peer2;
        this.height1 = height1;
        this.height2 = height2;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeerDiffStat)) return false;
        PeerDiffStat that = (PeerDiffStat) o;
        return diff1 == that.diff1 &&
                diff2 == that.diff2 &&
                height1 == that.height1 &&
                height2 == that.height2 &&
                Objects.equals(peer1, that.peer1) &&
                Objects.equals(peer2, that.peer2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diff1, diff2, peer1, peer2, height1, height2);
    }
}
