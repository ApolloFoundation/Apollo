/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;

public class DGSTag extends VersionedDerivedEntity {

    public void setInStockCount(int inStockCount) {
        this.inStockCount = inStockCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }


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

    public int getTotalCount() {
        return totalCount;
    }

}
