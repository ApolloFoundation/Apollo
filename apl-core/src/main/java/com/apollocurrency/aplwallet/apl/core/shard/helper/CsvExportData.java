/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

public class CsvExportData {
    private int processCount;
    private Object lastKey;

    public CsvExportData(int processCount, Object lastKey) {
        this.processCount = processCount;
        this.lastKey = lastKey;
    }

    public int getProcessCount() {
        return processCount;
    }

    public void setProcessCount(int processCount) {
        this.processCount = processCount;
    }

    public Object getLastKey() {
        return lastKey;
    }

    public void setLastKey(Object lastKey) {
        this.lastKey = lastKey;
    }
}
