/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.updater.*;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterDbRepository;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterServiceImpl;
import com.apollocurrency.aplwallet.apl.util.Listener;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;


public class UpdaterCoreImpl implements UpdaterCore {
    private static final Logger LOG = getLogger(UpdaterCoreImpl.class);

    private UpdaterService updaterService;
    private UpdaterMediator updaterMediator;
    private UpdaterFactory updaterFactory;
    private UpdateTransactionVerifier updateTransactionVerifier;
    private Listener<List<? extends Transaction>> listener;
    private UpdateInfo beforeUpdaterUpdateInfo;
    private volatile Updater lastUpdater;

    @Override
    public UpdateInfo getUpdateInfo() {
        return lastUpdater == null ?
                beforeUpdaterUpdateInfo == null ?
                        new UpdateInfo(false, 0, 0, null, Version.from("0.0.0"), UpdateInfo.UpdateState.NONE) : beforeUpdaterUpdateInfo :
                        lastUpdater.getUpdateInfo();
    }

    public UpdaterCoreImpl(UpdaterService updaterService, UpdaterMediator updaterMediator) {
        this(updaterService, updaterMediator, new UpdaterFactoryImpl(updaterMediator, updaterService),
                new UpdateTransactionVerifierImpl(updaterMediator, updaterService));
    }

    public UpdaterCoreImpl(UpdaterMediator updaterMediator) {
        this(new UpdaterServiceImpl(new UpdaterDbRepository(updaterMediator)), updaterMediator);
    }

    public UpdaterCoreImpl(UpdaterService updaterService, UpdaterMediator updaterMediator, UpdateTransactionVerifier updateTransactionVerifier) {
        this(updaterService, updaterMediator, new UpdaterFactoryImpl(updaterMediator, updaterService), updateTransactionVerifier);
    }

    public UpdaterCoreImpl(UpdaterService updaterService, UpdaterMediator updaterMediator, UpdaterFactory updaterFactory,
                           UpdateTransactionVerifier updateTransactionVerifier) {
        this.updaterService = updaterService;
        this.updaterMediator = updaterMediator;
        this.updaterFactory = updaterFactory;
        this.updateTransactionVerifier = updateTransactionVerifier;
        this.listener = new UpdateListener(updateTransactionVerifier);
    }
    
    @Override
    public void init() {
        UpdateTransaction updateTransaction = null;
        boolean startUpdater = true;
        try {
            updateTransaction = updaterService.getLast();
        }
        catch (Throwable e) {
            LOG.debug("Updater db error: ", e.getLocalizedMessage());
        }
        if (updateTransaction != null) {
            Transaction transaction = updateTransaction.getTransaction();
            if (!updateTransaction.isUpdated()) {
                LOG.debug("Found non-installed update : " + transaction.getJSONObject().toJSONString());
                UpdateData updateHolder = updateTransactionVerifier.process(transaction);
                if (updateHolder == null) {
                    LOG.error("Unable to validate update transaction: " + transaction.getJSONObject().toJSONString());
                    int deleted = updaterService.clear();
                    LOG.debug("Deleted {} invalid update transaction(s)", deleted);
                } else {
                    if (((TransactionType.Update) updateHolder.getTransaction().getType()).getLevel() != Level.MINOR) {
                        startUpdater = false;
                    }
                    startUpdate(updateHolder);
                }
            } else {
                Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
                Version expectedVersion = attachment.getAppVersion();
                if (expectedVersion.greaterThan(updaterMediator.getWalletVersion())) {
                    LOG.error("Found " + transaction.getType() + " update (platform dependent script failed): currentVersion: " + updaterMediator.getWalletVersion() +
                            " " + " updateVersion: " + expectedVersion);
                    if (transaction.getType() == TransactionType.Update.CRITICAL) {
                        updaterMediator.suspendBlockchain();
                        beforeUpdaterUpdateInfo = new UpdateInfo(true, 0,
                                transaction.getHeight(), Level.CRITICAL, expectedVersion);
                        beforeUpdaterUpdateInfo.setUpdateState(UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL);
                        startUpdater = false;
                        LOG.error("Manual install required for critical update!");
                    } else {
                        LOG.info("Skip uninstalled non-critical update");
                    }
                }
            }
        }
        if (startUpdater) {
            updaterMediator.addUpdateListener(listener);
        }
    }
    @Override
    public void startUpdate(UpdateData updateData) {
        updaterService.clearAndSave(new UpdateTransaction(updateData.getTransaction(), false));
        new Thread(() -> performUpdate(updateData), "Updater thread").start();
    }

    @Override
    public boolean startMinorUpdate() {
        if (lastUpdater != null && lastUpdater.getLevel() == Level.MINOR && lastUpdater.getUpdateInfo().getUpdateState() == UpdateInfo.UpdateState.REQUIRED_START && lastUpdater instanceof Startable) {
            new Thread(() -> ((Startable) lastUpdater).start(), "Minor update thread").start();
            return true;
        }
        return false;
    }

    private void performUpdate(UpdateData data) {
        lastUpdater = updaterFactory.getUpdater(data);
        lastUpdater.processUpdate();
    }
    

    public class UpdateListener implements Listener<List<? extends Transaction>> {
        private UpdateTransactionVerifier verifier;

        public UpdateListener(UpdateTransactionVerifier verifier) {
            this.verifier = verifier;
        }

        @Override
        public void notify(List<? extends Transaction> transactions) {
            transactions.forEach(transaction -> {
                UpdateData holder = verifier.process(transaction);
                if (holder != null) {
                    if (((TransactionType.Update) holder.getTransaction().getType()).getLevel() != Level.MINOR) {
                        updaterMediator.removeUpdateListener(this);
                    }
                    LOG.debug("Found appropriate update transaction: " + holder.getTransaction().getJSONObject().get("attachment"));
                    startUpdate(holder);
                }
            });
        }
    }

    @Override
    public void shutdown() {
        updaterMediator.removeUpdateListener(listener);
    }
}
