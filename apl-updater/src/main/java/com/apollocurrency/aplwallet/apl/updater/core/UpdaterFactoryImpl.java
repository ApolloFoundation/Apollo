/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.updater.UpdateData;
import com.apollocurrency.aplwallet.apl.updater.UpdaterConstants;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;

public class UpdaterFactoryImpl implements UpdaterFactory {
    private UpdaterMediator updaterMediator;
    private UpdaterService updaterService;

    public UpdaterFactoryImpl(UpdaterMediator updaterMediator, UpdaterService updaterService) {
        this.updaterMediator = updaterMediator;
        this.updaterService = updaterService;
    }

    @Override
    public Updater getUpdater(UpdateData updateDataHolder) {
        Level level = ((TransactionType.Update) updateDataHolder.getTransaction().getType()).getLevel();
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
