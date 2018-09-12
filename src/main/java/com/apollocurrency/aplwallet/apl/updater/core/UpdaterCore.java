/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.updater.UpdateData;
import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;

public interface UpdaterCore {

    void init();

    boolean startMinorUpdate();

    void startUpdate(UpdateData updateData);

    UpdateInfo getUpdateInfo();

    void shutdown();

}
