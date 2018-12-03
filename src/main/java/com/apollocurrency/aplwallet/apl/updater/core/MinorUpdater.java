/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.updater.UpdateData;
import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;

public class MinorUpdater extends AbstractUpdater {

    public MinorUpdater(UpdateData updateData, UpdaterService updaterService, UpdaterMediator updaterMediator) {
        super(updateData, updaterService, updaterMediator);
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
