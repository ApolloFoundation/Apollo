/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import java.util.List;

public class DerivedTableData<T> {
    private List<T> values;
    private long lastDbId;

    public DerivedTableData(List<T> values, long lastDbId) {
        this.values = values;
        this.lastDbId = lastDbId;
    }

    public List<T> getValues() {
        return values;
    }

    public void setValues(List<T> values) {
        this.values = values;
    }

    public long getLastDbId() {
        return lastDbId;
    }

    public void setLastDbId(long lastDbId) {
        this.lastDbId = lastDbId;
    }
}
