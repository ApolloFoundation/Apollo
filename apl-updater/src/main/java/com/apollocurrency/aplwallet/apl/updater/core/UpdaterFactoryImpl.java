/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import javax.inject.Inject;

import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.UpdaterConstants;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;

public class UpdaterFactoryImpl implements UpdaterFactory {
    private UpdaterMediator updaterMediator;
    private UpdaterService updaterService;

    @Inject
    public UpdaterFactoryImpl(UpdaterMediator updaterMediator, UpdaterService updaterService) {
        this.updaterMediator = updaterMediator;
        this.updaterService = updaterService;
    }

    @Override
    public Updater getUpdater(UpdateData updateDataHolder) {
        Level level = ((Update) updateDataHolder.getTransaction().getType()).getLevel();
        switch (level) {
            case CRITICAL :
                return new CriticalUpdater(updateDataHolder, updaterMediator, updaterService, 3, 200);
            case IMPORTANT:
                return new ImportantUpdater(updateDataHolder, updaterService, updaterMediator, UpdaterConstants.MIN_BLOCKS_DELAY,
                        UpdaterConstants.MAX_BLOCKS_DELAY);
            case MINOR:
                return new MinorUpdater(updateDataHolder, updaterService, updaterMediator);
            default:
                throw new IllegalArgumentException("Unable to construct updater for level: " + level);
        }
    }
}
