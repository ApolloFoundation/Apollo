/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.ScanEntity;

public interface ScanDao {
    void saveOrUpdate(ScanEntity scanEntity);

    ScanEntity get();
}
