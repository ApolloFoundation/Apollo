/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.UpdaterConstants;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;

import javax.inject.Inject;

public class UpdaterFactoryImpl implements UpdaterFactory {
    private UpdaterMediator updaterMediator;
    private UpdaterService updaterService;
    private UpdateInfo updateInfo;

    @Inject
    public UpdaterFactoryImpl(UpdaterMediator updaterMediator, UpdaterService updaterService, UpdateInfo updateInfo) {
        this.updaterMediator = updaterMediator;
        this.updaterService = updaterService;
        this.updateInfo = updateInfo;
    }

    @Override
    public Updater getUpdater(UpdateData updateDataHolder) {
        Level level = ((Update) updateDataHolder.getAttachment().getTransactionType()).getLevel();
        switch (level) {
            case CRITICAL :
                return new CriticalUpdater(updateDataHolder, updaterMediator, updaterService, 3, 200, updateInfo);
            case IMPORTANT:
                return new ImportantUpdater(updateDataHolder, updaterService, updaterMediator, UpdaterConstants.MIN_BLOCKS_DELAY,
                        UpdaterConstants.MAX_BLOCKS_DELAY, updateInfo);
            case MINOR:
                return new MinorUpdater(updateDataHolder, updaterService, updaterMediator, updateInfo);
            default:
                throw new IllegalArgumentException("Unable to construct updater for level: " + level);
        }
    }
}
