/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.updater.Startable;
import com.apollocurrency.aplwallet.apl.updater.UpdateData;
import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class MinorUpdater extends AbstractUpdater implements Startable {
    private static final Logger LOG = getLogger(MinorUpdater.class);

    public MinorUpdater(UpdateData updateData, UpdaterService updaterService, UpdaterMediator updaterMediator) {
        super(updateData, updaterService, updaterMediator);
    }

    @Override
    public UpdateInfo.UpdateState processUpdate() {
        LOG.info("Minor update is available. Required start by user");
        return UpdateInfo.UpdateState.REQUIRED_START;
    }

    @Override
    protected UpdateInfo.UpdateState getFailUpdateState() {
        return UpdateInfo.UpdateState.FAILED_REQUIRED_START;
    }

    @Override
    public Level getLevel() {
        return Level.MINOR;
    }

    @Override
    public void start() {
        super.processUpdate();
    }
}
