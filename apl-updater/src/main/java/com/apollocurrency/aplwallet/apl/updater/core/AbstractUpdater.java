/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransaction;
import com.apollocurrency.aplwallet.apl.updater.UpdaterConstants;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.pdu.PlatformDependentUpdater;
import com.apollocurrency.aplwallet.apl.updater.pdu.PlatformDependentUpdaterFactory;
import com.apollocurrency.aplwallet.apl.updater.pdu.PlatformDependentUpdaterFactoryImpl;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import org.slf4j.Logger;

public abstract class AbstractUpdater implements Updater {
    private static final Logger LOG = getLogger(AbstractUpdater.class);
    protected UpdateData updateData;
    protected UpdateInfo updateInfo;
    protected UpdaterMediator updaterMediator;
    protected UpdaterService updaterService;
    protected int blocksWait;
    protected int secondsWait;
    protected PlatformDependentUpdaterFactory pduFactory;

    public AbstractUpdater(UpdateData updateData, UpdaterService updaterService,
                           UpdaterMediator updaterMediator) {
        this(updateData, updaterMediator, updaterService, 0, 0);
    }

    public AbstractUpdater(UpdateData updateData, UpdaterMediator updaterMediator, UpdaterService updaterService, int blocksWait, int secondsWait) {
        this.updateData = updateData;
        this.updateInfo = new UpdateInfo(true,updateData.getTransaction().getId(), updateData.getTransaction().getHeight() + blocksWait,
                updateData.getTransaction().getHeight(), getLevel(),
                ((UpdateAttachment) updateData.getTransaction().getAttachment()).getAppVersion());
        this.updaterMediator = updaterMediator;
        this.updaterService = updaterService;
        this.blocksWait = blocksWait;
        this.secondsWait = secondsWait;
        this.pduFactory = new PlatformDependentUpdaterFactoryImpl(updaterMediator);
    }
    public AbstractUpdater(UpdateData updateData, UpdaterMediator updaterMediator, UpdaterService updaterService, int blocksWait, int secondsWait,
                           PlatformDependentUpdaterFactory pduFactory) {
        this.updateData = updateData;
        this.updateInfo = new UpdateInfo(true,updateData.getTransaction().getId(), updateData.getTransaction().getHeight() + blocksWait,
                updateData.getTransaction().getHeight(), getLevel(),
                ((UpdateAttachment) updateData.getTransaction().getAttachment()).getAppVersion(), UpdateInfo.UpdateState.NONE);
        this.updaterMediator = updaterMediator;
        this.updaterService = updaterService;
        this.blocksWait = blocksWait;
        this.secondsWait = secondsWait;
        this.pduFactory = pduFactory;

    }

    @Override
    public UpdateInfo.UpdateState processUpdate() {
        LOG.info("Starting {} update", getLevel());
        updateInfo.setUpdateState(UpdateInfo.UpdateState.IN_PROGRESS);
        LOG.debug("Waiting {} blocks or {} sec before starting update", blocksWait, secondsWait);
        waitBlocks(blocksWait, secondsWait);
        updaterMediator.suspendBlockchain();
        if (tryUpdate()) {
            LOG.info("{} was installed successfully");
            updaterService.update(new UpdateTransaction(updateData.getTransaction(), true));
            updateInfo.setUpdateState(UpdateInfo.UpdateState.FINISHED);
            return UpdateInfo.UpdateState.FINISHED;
        } else {
            LOG.error("Error occurred while installing {} update", getLevel());
            if (resumeBlockchainOnError()) {
                updaterMediator.resumeBlockchain();
            }
            updateInfo.setUpdateState(getFailUpdateState());
            return getFailUpdateState();
        }
    }

    protected boolean tryUpdate() {
        UpdateAttachment attachment = (UpdateAttachment) updateData.getTransaction().getAttachment();
        LOG.info("Update to version: " + attachment.getAppVersion());
        //Downloader downloads update package
        Path path = updaterService.tryDownload(updateData.getDecryptedUrl(), attachment.getHash());
        if (path != null) {
            if (updaterService.verifyJarSignature(UpdaterConstants.CERTIFICATE_DIRECTORY, path)) {
                try {
                    Path unpackedDirPath = updaterService.unpack(path);
                    PlatformDependentUpdater pdu = pduFactory.createInstance(Platform.current(), updateInfo);
                    pdu.start(unpackedDirPath);
                    return true;
                }
                catch (IOException e) {
                    LOG.error("Cannot unpack file: " + path.toString());
                }
            } else {
                LOG.error("Cannot verify jar signature!");
            }
        }
        return false;
    }

    private void waitBlocks(int blocks, int maxTime) {
        int currentHeight = updaterMediator.getBlockchainHeight();
        int targetHeight = currentHeight + blocks;
        int timeSpent = 0;
        while (currentHeight < targetHeight && timeSpent < maxTime) {
            try {
                TimeUnit.SECONDS.sleep(1);
                timeSpent++;
                currentHeight = updaterMediator.getBlockchainHeight();
            }
            catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    protected boolean resumeBlockchainOnError() {
        return true;
    }

    abstract protected UpdateInfo.UpdateState getFailUpdateState();

    @Override
    public UpdateInfo getUpdateInfo() {
        return updateInfo;
    }
}
