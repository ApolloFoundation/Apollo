/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.repository;

import com.apollocurrency.aplwallet.apl.updater.UpdateTransaction;

public interface UpdaterRepository {
    UpdateTransaction getLast();

    void save(UpdateTransaction transaction);

    void update(UpdateTransaction transaction);

    int clear();

    void clearAndSave(UpdateTransaction transaction);
}
