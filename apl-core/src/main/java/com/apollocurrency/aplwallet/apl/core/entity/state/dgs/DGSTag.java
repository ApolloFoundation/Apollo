/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.dgs;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;

import java.util.Objects;

public class DGSTag extends VersionedDerivedEntity {

    private final String tag;
    private int inStockCount;
    private int totalCount;

    public DGSTag(String tag, int height) {
        super(null, height);
        this.tag = tag;
    }

    public DGSTag(Long dbId, Integer height, String tag, int inStockCount, int totalCount) {
        super(dbId, height);
        this.tag = tag;
        this.inStockCount = inStockCount;
        this.totalCount = totalCount;
    }

    public String getTag() {
        return tag;
    }

    public int getInStockCount() {
        return inStockCount;
    }

    public void setInStockCount(int inStockCount) {
        this.inStockCount = inStockCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DGSTag)) return false;
        if (!super.equals(o)) return false;
        DGSTag dgsTag = (DGSTag) o;
        return inStockCount == dgsTag.inStockCount &&
            totalCount == dgsTag.totalCount &&
            Objects.equals(tag, dgsTag.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tag, inStockCount, totalCount);
    }
}
