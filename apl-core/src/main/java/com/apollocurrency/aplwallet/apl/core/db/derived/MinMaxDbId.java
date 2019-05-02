/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import java.util.Objects;

public class MinMaxDbId {
    private long minDbId = -1L;
    private long maxDbId = -1L;

    public MinMaxDbId(long minDbId, long maxDbId) {
        this.minDbId = minDbId;
        this.maxDbId = maxDbId;
    }

    public MinMaxDbId() {
    }

    public long getMinDbId() {
        return minDbId;
    }

    public void setMinDbId(long minDbId) {
        this.minDbId = minDbId;
    }

    public long getMaxDbId() {
        return maxDbId;
    }

    public void setMaxDbId(long maxDbId) {
        this.maxDbId = maxDbId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinMaxDbId that = (MinMaxDbId) o;
        return minDbId == that.minDbId &&
                maxDbId == that.maxDbId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minDbId, maxDbId);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MinMaxDbId{");
        sb.append("minDbId=").append(minDbId);
        sb.append(", maxDbId=").append(maxDbId);
        sb.append('}');
        return sb.toString();
    }
}
