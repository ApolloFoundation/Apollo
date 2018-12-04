/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.updater.UpdateData;

public interface UpdaterFactory {
    Updater getUpdater(UpdateData updateDataHolder);
}
