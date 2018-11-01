/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.CERTIFICATE_DIRECTORY;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.CERTIFICATE_SUFFIX;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.FIRST_DECRYPTION_CERTIFICATE_PREFIX;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.MAX_BLOCKS_WAITING;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.MIN_BLOCKS_WAITING;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.SECOND_DECRYPTION_CERTIFICATE_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.UpdateInfo;
import com.apollocurrency.aplwallet.apl.UpdaterDb;
import com.apollocurrency.aplwallet.apl.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.Version;
import com.apollocurrency.aplwallet.apl.updater.downloader.Downloader;
import com.apollocurrency.aplwallet.apl.util.Listener;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class UpdaterCore {
    private static final Logger LOG = getLogger(UpdaterCore.class);

    private volatile UpdateDataHolder updateDataHolder;
    private final Listener<List<? extends Transaction>> updateListener = this::processTransactions;

    private UpdaterCore() {
        Transaction transaction = null;
        boolean isUpdated = false;
        boolean startUpdater = true;
        try {
            transaction = UpdaterDb.loadLastUpdateTransaction();
            isUpdated = UpdaterDb.getUpdateStatus();
        }
        catch (Throwable e) {
            LOG.debug("Updater db error: ", e.getLocalizedMessage());
        }
        if (transaction != null) {
            if (!isUpdated) {
                LOG.debug("Found non-installed update : " + transaction.getJSONObject().toJSONString());
                UpdateDataHolder updateHolder = processTransaction(transaction);
                if (updateHolder == null) {
                    LOG.error("Unable to validate update transaction: " + transaction.getJSONObject().toJSONString());
                } else {
                    if (((TransactionType.Update) updateHolder.getTransaction().getType()).getLevel() != Level.MINOR) {
                        startUpdater = false;
                    }
                    this.updateDataHolder = updateHolder;
                    startUpdate();
                }
            } else {
                Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
                Version expectedVersion = attachment.getAppVersion();
                if (expectedVersion.greaterThan(Apl.VERSION)) {
                    LOG.error("Found " + transaction.getType() + " update (platform dependent script failed): currentVersion: " + Apl.VERSION +
                            " " + " updateVersion: " + expectedVersion);
                    if (transaction.getType() == TransactionType.Update.CRITICAL) {
//                        UpdaterMediator.getInstance().addUpdateListener(updateListener);
                        stopForgingAndBlockAcceptance();
                        UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL);
                        UpdaterMediator.getInstance().setUpdateData(true, 0,
                                transaction.getHeight(), Level.CRITICAL, expectedVersion);
                        startUpdater = false;
                        LOG.error("Manual install required for critical update!");
                    } else {
                        LOG.info("Skip uninstalled non-critical update");
                    }
                }
            }
        }
        if (startUpdater) {
            UpdaterMediator.getInstance().addUpdateListener(updateListener);
        }
    }

    public static UpdaterCore getInstance() {
        return UpdaterCoreHolder.HOLDER_INSTANCE;
    }

    public void startUpdate() {
        boolean isSaved = UpdaterDb.clearAndSaveUpdateTransaction(updateDataHolder.getTransaction().getId());
        if (!isSaved) {
            LOG.error("Unable to save update transaction to db!");
        }
        new Thread(() -> triggerUpdate(updateDataHolder), "Updater thread").start();
    }

    public void startMinorUpdate() {
        if (updateDataHolder.getTransaction().getType() == TransactionType.Update.MINOR) {
            Runnable minorUpdateTask = () -> {
                LOG.info("Starting minor update...");
                Transaction updateTransaction = updateDataHolder.getTransaction();
                TransactionType.Update type = (TransactionType.Update) updateTransaction.getType();
                Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) updateTransaction.getAttachment();
                int updateHeight = getUpdateHeightFromType(type);
                UpdaterMediator.getInstance().setUpdateData(true, updateHeight, updateTransaction.getHeight(), type.getLevel(), attachment.getAppVersion());
                UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.IN_PROGRESS);
                if (tryUpdate((Attachment.UpdateAttachment) updateDataHolder.getTransaction().getAttachment(), updateDataHolder.getDecryptedUrl())) {
                    LOG.info("Minor update was successfully installed ");
                    UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.FINISHED);
                } else {
                    LOG.error("Error! Cannot install minor update.");
                    UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.FAILED_REQUIRED_START);
                    UpdaterMediator.getInstance().restoreConnection();
                }
            };
            new Thread(minorUpdateTask, "Minor update thread").start();
        } else {
            throw new RuntimeException("Cannot start manually minor update for transaction type: " + updateDataHolder.getTransaction().getType());
        }
    }

    private void triggerUpdate(UpdateDataHolder holder) {
        Transaction updateTransaction = holder.getTransaction();
        TransactionType.Update type = (TransactionType.Update) updateTransaction.getType();
        Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) updateTransaction.getAttachment();
        int updateHeight = getUpdateHeightFromType(type);
        UpdaterMediator.getInstance().setUpdateData(true, updateHeight, updateTransaction.getHeight(), type.getLevel(), attachment.getAppVersion());
        UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.IN_PROGRESS);
        if (type == TransactionType.Update.CRITICAL) {
            //stop forging and peer server immediately
            LOG.warn("Starting critical update now!");
            if (tryUpdate(attachment, holder.getDecryptedUrl(), true)) {
                LOG.info("Critical update was successfully installed");
                UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.FINISHED);
            } else {
                LOG.error("FAILURE! Cannot install critical update.");
                UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL);
                UpdaterMediator.getInstance().restoreConnection();
            }
        } else if (type == TransactionType.Update.IMPORTANT) {
            boolean updated = false;
            while (!updated) {
                updated = scheduleUpdate(updateHeight, attachment, holder.getDecryptedUrl());
                if (!updated) {
                    UpdaterMediator.getInstance().restoreConnection();
                    updateHeight = getUpdateHeightFromType(type);
                    LOG.error("Cannot install scheduled important update. Trying to schedule new update attempt at " + updateHeight + " height");
                    UpdaterMediator.getInstance().setUpdateHeight(updateHeight);
                    UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.RE_PLANNING);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    }
                    catch (InterruptedException e) {
                        LOG.error("Important update exception", e);
                    }
                }
            }
            LOG.info("Important update was installed successfully!");
            UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.FINISHED);
        } else if (type == TransactionType.Update.MINOR) {
            LOG.info("Minor update is available. Required start by user");
            UpdaterMediator.getInstance().setUpdateState(UpdateInfo.UpdateState.REQUIRED_START);
        }
    }

    private boolean scheduleUpdate(int updateHeight, Attachment.UpdateAttachment attachment, String decryptedUrl) {
        LOG.info("Update estimated height: ", updateHeight);
        while (UpdaterMediator.getInstance().getBlockchainHeight() < updateHeight) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            }
            catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        LOG.info("Starting scheduled update. CurrentHeight: " + UpdaterMediator.getInstance().getBlockchainHeight() + " updateHeight: " + updateHeight);
        return tryUpdate(attachment, decryptedUrl);
    }

    private void waitBlocks(int blocks, int maxTime) {
        int currentHeight = UpdaterMediator.getInstance().getBlockchainHeight();
        int targetHeight = currentHeight + blocks;
        int timeSpent = 0;
        while (currentHeight < targetHeight && timeSpent < maxTime) {
            try {
                TimeUnit.SECONDS.sleep(1);
                timeSpent++;
                currentHeight = UpdaterMediator.getInstance().getBlockchainHeight();
            }
            catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private boolean tryUpdate(Attachment.UpdateAttachment attachment, String decryptedUrl) {
        return tryUpdate(attachment, decryptedUrl, false);
    }

    private boolean tryUpdate(Attachment.UpdateAttachment attachment, String decryptedUrl, boolean isWait) {
        if (isWait) {
            LOG.info("Waiting 3 blocks or 200 sec for starting update");
            waitBlocks(3, 200);
        }
        LOG.info("Update to version: " + attachment.getAppVersion());
        stopForgingAndBlockAcceptance();
        //Downloader downloads update package
        Path path = Downloader.getInstance().tryDownload(decryptedUrl, attachment.getHash());
        if (path != null) {
            if (verifyJar(path)) {
                try {
                    Path unpackedDirPath = Unpacker.getInstance().unpack(path);
                    PlatformDependentUpdater.getInstance().continueUpdate(unpackedDirPath, attachment.getPlatform());
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


    private void processTransactions(List<? extends Transaction> transactions) {
        transactions.forEach(transaction -> {
            UpdateDataHolder holder = processTransaction(transaction);
            if (holder != null) {
                if (((TransactionType.Update) holder.getTransaction().getType()).getLevel() != Level.MINOR) {
                    UpdaterMediator.getInstance().removeListener(updateListener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
                }
                LOG.debug("Found appropriate update transaction: " + holder.getTransaction().getJSONObject().get("attachment"));
                this.updateDataHolder = holder;
                startUpdate();
            }
        });
    }

    private UpdateDataHolder processTransaction(Transaction tr) {
        if (UpdaterMediator.getInstance().isUpdateTransaction(tr)) {
            LOG.debug("Processing update transaction " + tr.getId());
            Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) tr.getAttachment();
            if (attachment.getAppVersion().greaterThan(UpdaterMediator.getInstance().getWalletVersion())) {
                Platform currentPlatform = Platform.current();
                Architecture currentArchitecture = Architecture.current();
                if (attachment.getPlatform() == currentPlatform && attachment.getArchitecture() == currentArchitecture) {
                    String url = RSAUtil.tryDecryptUrl(CERTIFICATE_DIRECTORY, attachment.getUrl(), attachment.getAppVersion());
                    if (url != null && !url.isEmpty()) {
                        if (AuthorityChecker.getInstance().verifyCertificates(CERTIFICATE_DIRECTORY)) {
                            return new UpdateDataHolder(tr, url);
                        } else {
                            LOG.error("Cannot verify certificates!");
                            SecurityAlertSender.getInstance().send("Certificate verification error" + tr.getJSONObject().toJSONString());
                        }
                    } else {
                        LOG.error("Cannot decrypt url for update transaction:" + tr.getId());
                        SecurityAlertSender.getInstance().send("Cannot decrypt url for update transaction:" + tr.getId());
                    }
                }
            }
        }
        return null;
    }


    private boolean verifyJar(Path jarFilePath) {
        try {
            Set<Certificate> certificates = UpdaterUtil.readCertificates(CERTIFICATE_DIRECTORY, CERTIFICATE_SUFFIX, FIRST_DECRYPTION_CERTIFICATE_PREFIX, SECOND_DECRYPTION_CERTIFICATE_PREFIX);
            for (Certificate certificate : certificates) {
                try {
                    AuthorityChecker.getInstance().verifyJarSignature(certificate, jarFilePath);
                    return true;
                }
                catch (SecurityException e) {
                    LOG.warn("Certificate is not appropriate." + UpdaterUtil.getStringRepresentation(certificate));
                }
            }
        }
        catch (CertificateException | IOException | URISyntaxException e) {
            LOG.error("Unable to load certificates");
        }
        return false;
    }

    private void stopForgingAndBlockAcceptance() {
        LOG.debug("Suspending forging...");
        UpdaterMediator.getInstance().stopForging();
        LOG.info("Forging was suspended!");
        LOG.debug("Suspending peer server...");
        UpdaterMediator.getInstance().shutdownPeerServer();
        LOG.info("Peer server was suspended");
        LOG.debug("Suspend blockchain processor...");
        UpdaterMediator.getInstance().shutdownBlockchainProcessor();
        LOG.info("Blockchain processor was suspended");
    }

    private int getUpdateHeightFromType(TransactionType type) {
        return
                //update is NOW on currentBlockchainHeight
                type == TransactionType.Update.CRITICAL ? UpdaterMediator.getInstance().getBlockchainHeight() :

                        // update height = currentBlockchainHeight + random number in range [MIN_BLOCKS_WAITING...MAX_BLOCKS_WAITING]
                        type == TransactionType.Update.IMPORTANT ? new Random().nextInt(MAX_BLOCKS_WAITING - MIN_BLOCKS_WAITING)
                                + MIN_BLOCKS_WAITING + UpdaterMediator.getInstance().getBlockchainHeight() :

                                //assume that current update is not mandatory
                                type == TransactionType.Update.MINOR ? -1 : 0;
    }

    public static class UpdateDataHolder {
        private Transaction transaction;
        private String decryptedUrl;

        public UpdateDataHolder(Transaction transaction, String decryptedUrl) {
            this.transaction = transaction;
            this.decryptedUrl = decryptedUrl;
        }

        @Override
        public String toString() {
            return "UpdateDataHolder{" +
                    "transaction=" + transaction.getJSONObject().toJSONString() +
                    ", decryptedUrl='" + decryptedUrl + '\'' +
                    '}';
        }

        public Transaction getTransaction() {
            return transaction;
        }

        private void setTransaction(Transaction transaction) {
            this.transaction = transaction;
        }

        public String getDecryptedUrl() {
            return decryptedUrl;
        }

        private void setDecryptedUrl(String decryptedUrl) {
            this.decryptedUrl = decryptedUrl;
        }
    }

    private static class UpdaterCoreHolder {
        private static final UpdaterCore HOLDER_INSTANCE = new UpdaterCore();
    }
}
