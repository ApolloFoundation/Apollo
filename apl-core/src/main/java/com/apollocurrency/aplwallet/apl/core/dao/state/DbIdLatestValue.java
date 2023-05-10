/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DbIdLatestValue {
    private int height;
    private boolean latest;
    private boolean deleted;
    private long dbId;

    public void makeDeleted() {
        latest = false;
        deleted = true;
    }

    public void makeVersioned() {
        latest = false;
        deleted = false;
    }

    public void makeLatest() {
        latest = true;
        deleted = false;
    }
}
