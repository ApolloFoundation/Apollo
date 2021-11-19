/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.appdata;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScanEntity {
    private boolean rescan = true;
    private boolean validate;
    private int fromHeight;
    private int currentScanHeight;
    private boolean preparationDone;
    private boolean shutdown;

    public ScanEntity(boolean validate, int fromHeight, boolean shutdown) {
        this.validate = validate;
        this.fromHeight = fromHeight;
        this.shutdown = shutdown;
        this.currentScanHeight = fromHeight;
    }

}
