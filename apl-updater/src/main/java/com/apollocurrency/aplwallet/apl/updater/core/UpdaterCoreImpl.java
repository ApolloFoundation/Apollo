/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.UpdaterMediatorImpl;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.update.UpdateTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransaction;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransactionVerifier;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransactionVerifierImpl;
import com.apollocurrency.aplwallet.apl.updater.UpdaterUtil;
import com.apollocurrency.aplwallet.apl.updater.export.event.UpdateEvent;
import com.apollocurrency.aplwallet.apl.updater.export.event.UpdateEventBinding;
import com.apollocurrency.aplwallet.apl.updater.export.event.UpdateEventData;
import com.apollocurrency.aplwallet.apl.updater.export.event.UpdateEventType;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterDbRepository;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterServiceImpl;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class UpdaterCoreImpl implements UpdaterCore {
    private static final Logger LOG = getLogger(UpdaterCoreImpl.class);
    private final UpdateInfo updateInfo;
    private final Event<UpdateEventData> startUpdateEvent;
    private UpdaterService updaterService;
    private UpdaterMediator updaterMediator;
    private UpdaterFactory updaterFactory;
    private UpdateTransactionVerifier updateTransactionVerifier;
    private volatile boolean processUpdateTxs = false;

    public UpdaterCoreImpl(UpdaterService updaterService, UpdaterMediator updaterMediator, UpdateInfo updateInfo, Event<UpdateEventData> startUpdateEvent) {
        this(updaterService, updaterMediator, new UpdaterFactoryImpl(updaterMediator, updaterService, updateInfo),
            new UpdateTransactionVerifierImpl(updaterMediator, updaterService), updateInfo, startUpdateEvent);
    }

    @Inject
    public UpdaterCoreImpl(UpdaterMediatorImpl updaterMediator, UpdateInfo updateInfo, Event<UpdateEventData> startUpdateEvent) {
        this(new UpdaterServiceImpl(new UpdaterDbRepository(updaterMediator)), updaterMediator, updateInfo, startUpdateEvent);
    }

    public UpdaterCoreImpl(UpdaterService updaterService, UpdaterMediator updaterMediator,
                           UpdateTransactionVerifier updateTransactionVerifier, UpdateInfo updateInfo, Event<UpdateEventData> startUpdateEvent) {
        this(updaterService, updaterMediator,
            new UpdaterFactoryImpl(updaterMediator, updaterService, updateInfo),
            updateTransactionVerifier, updateInfo, startUpdateEvent);
    }


    public UpdaterCoreImpl(UpdaterService updaterService, UpdaterMediator updaterMediator, UpdaterFactory updaterFactory,
                           UpdateTransactionVerifier updateTransactionVerifier, UpdateInfo updateInfo, Event<UpdateEventData> startUpdateEvent) {
        this.updaterService = updaterService;
        this.updaterMediator = updaterMediator;
        this.updaterFactory = updaterFactory;
        this.updateInfo = Objects.requireNonNull(updateInfo);
        this.updateTransactionVerifier = updateTransactionVerifier;
        this.startUpdateEvent = startUpdateEvent;
    }

    @Override
    public UpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    @Override
    public void init() {
        init(null, false);
    }

    @Override
    public void init(String updateAttachmentFile, boolean debug) {
        UpdaterUtil.init(debug);
        UpdateTransaction updateTransaction = null;
        boolean startUpdater = true;
        if (!StringUtils.isBlank(updateAttachmentFile)) {
            UpdateAttachment updateAttachment = loadFromFile(updateAttachmentFile);
            startUpdater = false;
            UpdateData updateData = updateTransactionVerifier.process(updateAttachment, -1);
            if (updateData == null) {
                LOG.debug("Unable to validate file attachment");
            } else {
                fireStartUpdateEventMethod(); // fire event for doing data export
                LOG.debug("run startUpdate...");
                startUpdate(updateData);
            }
        } else {
            try {
                updateTransaction = updaterService.getLast();
            } catch (Throwable e) {
                LOG.debug("Updater db error: {}", e.getLocalizedMessage());
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
                            updateState(transaction, UpdateInfo.UpdateState.REQUIRED_START);
                        }
                    }
                } else {
                    UpdateAttachment attachment = (UpdateAttachment) transaction.getAttachment();
                    Version expectedVersion = attachment.getAppVersion();
                    if (expectedVersion.greaterThan(updaterMediator.getWalletVersion())) {
                        LOG.error("Found " + transaction.getType() + " update (platform dependent script failed): currentVersion: " + updaterMediator.getWalletVersion() +
                            " " + " updateVersion: " + expectedVersion);
                        if (transaction.getType() == UpdateTransactionType.CRITICAL) {
                            updaterMediator.suspendBlockchain();
                            updateState(transaction, UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL);
                            startUpdater = false;
                            LOG.error("Manual install required for critical update!");
                        } else {
                            LOG.info("Skip uninstalled non-critical update");
                        }
                    }
                }
            }
        }
        processUpdateTxs = startUpdater;
    }

    private UpdateAttachment loadFromFile(String updateAttachmentFile) {
        try {
            Path path = Paths.get(updateAttachmentFile);
            ObjectMapper objectMapper = new ObjectMapper();
            FileUpdateAttachment fua = objectMapper.readValue(path.toAbsolutePath().toFile(), FileUpdateAttachment.class);
            UpdateAttachment attachment = UpdateAttachment.getAttachment(fua.getPlatform(), fua.getArchitecture(), new DoubleByteArrayTuple(
                    Convert.parseHexString(fua.getUrlFirstPart()), Convert.parseHexString(fua.getUrlSecondPart())
                ),
                new Version(fua.getVersion()),
                Convert.parseHexString(fua.getHash()),
                (byte) fua.getLevel()
            );
            LOG.info("Got update attachment: {}", attachment.getJSONObject().toJSONString());
            return attachment;
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
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
        getUpdateInfo().setDownloadInfo(updaterService.getDownloadInfo());
        fireStartUpdateEventMethod(); // fire event for doing data export
        UpdateInfo.UpdateState updateState = updater.processUpdate();
        LOG.info("Update state: {}", updateState);
    }

    public void onTransactionsConfirmed(@Observes @TxEvent(TxEventType.ADDED_CONFIRMED_TRANSACTIONS) List<Transaction> transactions) {
        if (processUpdateTxs) {
            transactions.forEach(transaction -> {
                UpdateData updateData = updateTransactionVerifier.process(transaction);
                if (updateData != null) {
                    LOG.debug("Found appropriate update transaction: " + updateData.getAttachment().getJSONObject().toJSONString());
                    updaterService.clearAndSave(new UpdateTransaction(updateData.getTransactionId(), false));
                    if (updateData.isAutomaticUpdate()) {
                        processUpdateTxs = false;
                        startUpdate(updateData);
                    } else {
                        updateState(updateData, UpdateInfo.UpdateState.REQUIRED_START);
                    }
                }
            });
        }
    }

    private void updateState(UpdateData updateData, UpdateInfo.UpdateState state) {
        UpdateAttachment updateAttachment = updateData.getAttachment();
        synchronized (updateInfo) {
            updateInfo.setUpdate(true);
            updateInfo.setId(updateData.getTransactionId());
            updateInfo.setLevel(((UpdateTransactionType) updateAttachment.getTransactionType()).getLevel());
            updateInfo.setVersion(updateAttachment.getAppVersion());
            updateInfo.setUpdateState(state);
        }
    }

    private void updateState(Transaction transaction, UpdateInfo.UpdateState state) {
        UpdateAttachment updateAttachment = (UpdateAttachment) transaction.getAttachment();
        synchronized (updateInfo) {
            updateInfo.setUpdate(true);
            updateInfo.setId(transaction.getId());
            updateInfo.setVersion(updateAttachment.getAppVersion());
            updateInfo.setUpdateState(state);
        }
    }

    protected void fireStartUpdateEventMethod() {
        LOG.debug("Fire StartUpdateEvent BEFORE...");
        UpdateEventData updateEventData = new UpdateEventData();
        startUpdateEvent.select(literal(UpdateEventType.BEFORE_SCRIPT)).fire(updateEventData); // data is ignored
    }

    private AnnotationLiteral<UpdateEvent> literal(UpdateEventType updateEventType) {
        return new UpdateEventBinding() {
            @Override
            public UpdateEventType value() {
                return updateEventType;
            }
        };
    }
}
