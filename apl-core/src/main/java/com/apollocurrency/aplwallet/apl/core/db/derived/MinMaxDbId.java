/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import java.util.Objects;

public class MinMaxDbId {
    private long minDbId = -1L;
    private long maxDbId = -1L;
    private long count;

    public MinMaxDbId(long minDbId, long maxDbId) {
        this.minDbId = minDbId;
        this.maxDbId = maxDbId;
    }

    public MinMaxDbId(long minDbId, long maxDbId, long count) {
        this.minDbId = minDbId;
        this.maxDbId = maxDbId;
        this.count = count;
    }

    public MinMaxDbId() {
    }

    public long getMinDbId() {
        return minDbId;
    }

    public void setMinDbId(long minDbId) {
        this.minDbId = minDbId;
    }

    public void incrementMin() {
        this.minDbId++;
    }

    public long getMaxDbId() {
        return maxDbId;
    }

    public void setMaxDbId(long maxDbId) {
        this.maxDbId = maxDbId;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinMaxDbId that = (MinMaxDbId) o;
        return minDbId == that.minDbId
                && maxDbId == that.maxDbId
                && count == that.count;
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
        sb.append(", count=").append(count);
        sb.append('}');
        return sb.toString();
    }

    public static MinMaxDbIdBuilder builder() {
        return new MinMaxDbIdBuilder();
    }

    public static final class MinMaxDbIdBuilder {
        private long minDbId = -1L;
        private long maxDbId = -1L;
        private long count;

        private MinMaxDbIdBuilder() {
        }

        public MinMaxDbIdBuilder minDbId(Long minDbId) {
            this.minDbId = minDbId;
            return this;
        }

        public MinMaxDbIdBuilder maxDbId(long maxDbId) {
            this.maxDbId = maxDbId;
            return this;
        }

        public MinMaxDbIdBuilder count(Long count) {
            this.count = count;
            return this;
        }

        public MinMaxDbId build() {
            return new MinMaxDbId(minDbId, maxDbId, count);
        }
    }
}
