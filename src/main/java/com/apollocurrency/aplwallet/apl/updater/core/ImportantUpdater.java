/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.updater.UpdateData;
import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import org.slf4j.Logger;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class ImportantUpdater extends AbstractUpdater {
    private static final Logger LOG = getLogger(ImportantUpdater.class);
    private int minBlocksDelay;
    private int maxBlocksDelay;

    public ImportantUpdater(UpdateData updateDataHolder, UpdaterService updaterService, UpdaterMediator updaterMediator, int minBlocksDelay,
                            int maxBlocksDelay) {
        super(updateDataHolder, updaterService, updaterMediator);
        this.minBlocksDelay = minBlocksDelay;
        this.maxBlocksDelay = maxBlocksDelay;
    }

    @Override
    public UpdateInfo.UpdateState processUpdate() {
        Random random = new Random();
        UpdateInfo.UpdateState currentState = UpdateInfo.UpdateState.NONE;
        while (currentState != UpdateInfo.UpdateState.FINISHED) {
            int updateHeight = updaterMediator.getBlockchainHeight() + random.nextInt(maxBlocksDelay - minBlocksDelay) + minBlocksDelay;
            updateInfo.setEstimatedHeight(updateHeight);
            waitHeight(updateHeight);
            currentState = super.processUpdate();
            }
        return currentState;
    }

    @Override
    protected UpdateInfo.UpdateState getFailUpdateState() {
        return UpdateInfo.UpdateState.RE_PLANNING;
    }

    private void waitHeight(int updateHeight) {
        LOG.info("Update estimated height: ", updateHeight);
        while (updaterMediator.getBlockchainHeight() < updateHeight) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            }
            catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public Level getLevel() {
        return Level.IMPORTANT;
    }
}
