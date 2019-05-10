/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

public class DGSTag {
    public DbKey getDbKey() {
        return dbKey;
    }

    public void setInStockCount(int inStockCount) {
        this.inStockCount = inStockCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    private final String tag;
    private DbKey dbKey;
    private int inStockCount;
    private int totalCount;
    private int height;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public DGSTag(String tag) {
        this.tag = tag;
    }

    public DGSTag(String tag, int inStockCount, int totalCount, int height) {
        this.tag = tag;
        this.inStockCount = inStockCount;
        this.totalCount = totalCount;
        this.height = height;
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
