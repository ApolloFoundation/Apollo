/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;

public class CriticalUpdater extends AbstractUpdater {

    public CriticalUpdater(UpdateData updateDataHolder, UpdaterMediator updaterMediator, UpdaterService updaterService, int blocksWait, int secondsWait, UpdateInfo updateInfo) {
        super(updateDataHolder, updaterMediator, updaterService, blocksWait, secondsWait, updateInfo);
    }

    @Override
    protected UpdateInfo.UpdateState getFailUpdateState() {
        return UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL;
    }

    @Override
    public Level getLevel() {
        return Level.CRITICAL;
    }

    @Override
    protected boolean resumeBlockchainOnError() {
        return false;
    }
}
