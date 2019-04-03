/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.app.UpdaterMediatorImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransaction;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransactionVerifier;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransactionVerifierImpl;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterDbRepository;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterServiceImpl;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UpdaterCoreImpl implements UpdaterCore {
    private static final Logger LOG = getLogger(UpdaterCoreImpl.class);
    private UpdaterService updaterService;
    private UpdaterMediator updaterMediator;
    private UpdaterFactory updaterFactory;
    private UpdateTransactionVerifier updateTransactionVerifier;
    private UpdateTransactionProcessingListener updateTransactionProcessingListener;
    private Listener<UpdateData> updatePerformingListener;
    private AtomicReference<UpdateInfo> updateInfo;


    private void setUpdateInfo(UpdateInfo updateInfo) {
        this.updateInfo.getAndSet(updateInfo);
    }

    @Override
    public UpdateInfo getUpdateInfo() {
        return updateInfo.get();
    }

    public UpdaterCoreImpl(UpdaterService updaterService, UpdaterMediator updaterMediator) {
        this(updaterService, updaterMediator, new UpdaterFactoryImpl(updaterMediator, updaterService),
                new UpdateTransactionVerifierImpl(updaterMediator, updaterService));
    }

    @Inject
    public UpdaterCoreImpl(UpdaterMediatorImpl updaterMediator) {
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
        this.updateTransactionProcessingListener = new UpdateTransactionProcessingListener(updateTransactionVerifier);
        this.updatePerformingListener = new UpdatePerformingListener();
        this.updateInfo = new AtomicReference<>(new UpdateInfo());
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
                UpdateData updateData = updateTransactionVerifier.process(transaction);
                if (updateData == null) {
                    LOG.error("Unable to validate update transaction: " + transaction.getJSONObject().toJSONString());
                    int deleted = updaterService.clear();
                    LOG.debug("Deleted {} invalid update transaction(s)", deleted);
                } else {
                    if (updateData.isAutomaticUpdate()) {
                        startUpdater = false;
                        startUpdate(updateData);
                    } else {
                        setUpdateInfo(transactionToUpdateInfo(transaction, UpdateInfo.UpdateState.REQUIRED_START));
                    }
                }
            } else {
                UpdateAttachment attachment = (UpdateAttachment) transaction.getAttachment();
                Version expectedVersion = attachment.getAppVersion();
                if (expectedVersion.greaterThan(updaterMediator.getWalletVersion())) {
                    LOG.error("Found " + transaction.getType() + " update (platform dependent script failed): currentVersion: " + updaterMediator.getWalletVersion() +
                            " " + " updateVersion: " + expectedVersion);
                    if (transaction.getType() == Update.CRITICAL) {
                        updaterMediator.suspendBlockchain();
                        UpdateInfo currentUpdateInfo = transactionToUpdateInfo(transaction, UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL);
                        setUpdateInfo(currentUpdateInfo);
                        startUpdater = false;
                        LOG.error("Manual install required for critical update!");
                    } else {
                        LOG.info("Skip uninstalled non-critical update");
                    }
                }
            }
        }
        if (startUpdater) {
            registerListeners();
        }
    }

    @Override
    public void startUpdate(UpdateData updateData) {
        new Thread(() ->
                performUpdate(updateData),
                "UpdateExecutor").start();
    }

    @Override
    public boolean startAvailableUpdate() {
        UpdateTransaction last = updaterService.getLast();
        if (last == null) {
            return false;
        }
        if (last.isUpdated()) {
            return false;
        }
        UpdateData updateData = updateTransactionVerifier.process(last.getTransaction());
        if (updateData == null) {
            return false;
        }
        startUpdate(updateData);
        return true;
    }

    private void performUpdate(UpdateData data) {
        Updater updater = updaterFactory.getUpdater(data);
        setUpdateInfo(updater.getUpdateInfo());
        getUpdateInfo().setDownloadInfo(updaterService.getDownloadInfo());
        UpdateInfo.UpdateState updateState = updater.processUpdate();
        LOG.info("Update state: {}", updateState);
    }


    public class UpdateTransactionProcessingListener implements Listener<List<? extends Transaction>> {
        private UpdateTransactionVerifier verifier;
        private final Listeners<UpdateData, UpdateEvent> listeners = new Listeners<>();
        public UpdateTransactionProcessingListener(UpdateTransactionVerifier verifier) {
            this.verifier = verifier;
        }

        public void addListener(UpdateEvent event, Listener<UpdateData> updateDataListener) {
            listeners.addListener(updateDataListener, event);
        }
        public void removeListener(UpdateEvent event, Listener<UpdateData> updateDataListener) {
            listeners.removeListener(updateDataListener, event);
        }
        @Override
        public void notify(List<? extends Transaction> transactions) {
            transactions.forEach(transaction -> {
                UpdateData updateData = verifier.process(transaction);
                if (updateData != null) {
                    if (((Update) updateData.getTransaction().getType()).getLevel() != Level.MINOR) {
                        updaterMediator.removeUpdateListener(this);
                    }
                    LOG.debug("Found appropriate update transaction: " + updateData.getTransaction().getJSONObject().get("attachment"));
                    listeners.notify(updateData, UpdateEvent.NEW_UPDATE);
                }
            });
        }
    }

    private class UpdatePerformingListener implements Listener<UpdateData> {

        @Override
        public void notify(UpdateData updateData) {
            updaterService.clearAndSave(new UpdateTransaction(updateData.getTransaction(), false));
            if (updateData.isAutomaticUpdate()) {
                deregisterUpdateProcessingListener();
                startUpdate(updateData);
            } else {
                UpdateInfo updateInfo = transactionToUpdateInfo(updateData.getTransaction(), UpdateInfo.UpdateState.REQUIRED_START);
                setUpdateInfo(updateInfo);
            }
        }
    }

    private UpdateInfo transactionToUpdateInfo(Transaction transaction, UpdateInfo.UpdateState state) {;
        UpdateAttachment updateAttachment = (UpdateAttachment) transaction.getAttachment();
        return new UpdateInfo(true,
                transaction.getId(),
                0,
                updaterMediator.getBlockchainHeight(),
                from(transaction.getType()),
                updateAttachment.getAppVersion(),
                state);
    }

    public enum UpdateEvent {
        NEW_UPDATE
    }

    private Level from(TransactionType type) {

        if (type == Update.CRITICAL) {
            return Level.CRITICAL;
        } else if (type == Update.IMPORTANT) {
            return Level.IMPORTANT;
        } else if (type == Update.MINOR) {
            return Level.MINOR;
        }
        return null;
    }

    @Override
    public void shutdown() {
        deregisterListeners();
    }

    private void deregisterUpdateProcessingListener() {
        updaterMediator.removeUpdateListener(updateTransactionProcessingListener);
    }

    private void registerListeners() {
        updateTransactionProcessingListener.addListener(UpdateEvent.NEW_UPDATE, updatePerformingListener);
        updaterMediator.addUpdateListener(updateTransactionProcessingListener);
    }

    private void deregisterListeners() {
        deregisterUpdateProcessingListener();
        updateTransactionProcessingListener.removeListener(UpdateEvent.NEW_UPDATE, updatePerformingListener);
    }
}
