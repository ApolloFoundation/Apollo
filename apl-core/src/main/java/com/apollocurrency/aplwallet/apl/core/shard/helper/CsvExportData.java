/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.util.Map;

public class CsvExportData {
    private int processCount;
    private Map<String, Object> lastRow;

    public CsvExportData(int processCount, Map<String, Object> lastRow) {
        this.processCount = processCount;
        this.lastRow = lastRow;
    }

    public int getProcessCount() {
        return processCount;
    }

    public void setProcessCount(int processCount) {
        this.processCount = processCount;
    }

    public Map<String, Object> getLastRow() {
        return lastRow;
    }

    public void setLastRow(Map<String, Object> lastRow) {
        this.lastRow = lastRow;
    }

    public int getRowCount() {
        if (this.lastRow != null) {
            return this.lastRow.size();
        } else {
            return -1;
        }
    }
}
