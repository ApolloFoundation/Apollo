/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;

public class MinorUpdater extends AbstractUpdater {

    public MinorUpdater(UpdateData updateData, UpdaterService updaterService, UpdaterMediator updaterMediator, UpdateInfo updateInfo) {
        super(updateData, updaterService, updaterMediator, updateInfo);
    }

    @Override
    protected UpdateInfo.UpdateState getFailUpdateState() {
        return UpdateInfo.UpdateState.FAILED_REQUIRED_START;
    }

    @Override
    public Level getLevel() {
        return Level.MINOR;
    }

}
