/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.updater.UpdateData;
import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;

public class CriticalUpdater extends AbstractUpdater {

    public CriticalUpdater(UpdateData updateDataHolder, UpdaterMediator updaterMediator, UpdaterService updaterService, int blocksWait, int secondsWait) {
        super(updateDataHolder, updaterMediator, updaterService, blocksWait, secondsWait);
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
