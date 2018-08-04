/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.updater;

import apl.*;
import apl.util.Listener;
import apl.util.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static apl.updater.UpdaterUtil.CertificatePair;

public class UpdaterCore {
    private static final String URL_TEMPLATE = "(http)|(https)//:.+/ApolloWallet-%s.jar";
    private final UpdaterMediator mediator = UpdaterMediator.getInstance();
    private final Downloader downloader = Downloader.getInstance();
    private final SecurityAlertSender alertSender = SecurityAlertSender.getInstance();
    private final AuthorityChecker checker = AuthorityChecker.getInstance();
    private final Unpacker unpacker = Unpacker.getInstance();
    private final PlatformDependentUpdater platformDependentUpdater = new PlatformDependentUpdater();
    private volatile UpdateDataHolder updateDataHolder;
    private final Listener<List<? extends Transaction>> updateListener = this::processTransactions;

    private UpdaterCore() {
        mediator.addUpdateListener(updateListener);
    }

    public static UpdaterCore getInstance() {
        return UpdaterCoreHolder.HOLDER_INSTANCE;
    }

    public void stopForgingAndBlockAcceptance() {
        Logger.logDebugMessage("Stopping forging...");
        int numberOfGenerators = mediator.stopForging();
        Logger.logInfoMessage("Forging was stopped, total generators: " + numberOfGenerators);
        Logger.logDebugMessage("Shutdown peer server...");
        mediator.shutdownPeerServer();
        Logger.logInfoMessage("Peer server was shutdown");
        Logger.logDebugMessage("Shutdown blockchain processor...");
        mediator.shutdownBlockchainProcessor();
        Logger.logInfoMessage("Blockchain processor was shutdown");
    }

    public void startUpdate() {
        new Thread(() -> triggerUpdate(updateDataHolder), "Updater thread").start();
    }

    public void triggerUpdate(UpdateDataHolder holder) {
        Transaction updateTransaction = holder.getTransaction();
        TransactionType type = updateTransaction.getType();
        Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) updateTransaction.getAttachment();
        int updateHeight = getUpdateHeightFromType(type);
        mediator.setUpdateData(true, updateHeight, updateTransaction.getHeight(), attachment.getLevel(), attachment.getAppVersion());
        mediator.setUpdateState(UpdateInfo.UpdateState.IN_PROGRESS);
        if (type == TransactionType.Update.CRITICAL) {
            //stop forging and peer server immediately
            Logger.logWarningMessage("Starting critical update now!");
            if (tryUpdate(attachment, holder.getDecryptedUrl())) {
                Logger.logInfoMessage("Critical update was successfully installed");
                mediator.setUpdateState(UpdateInfo.UpdateState.FINISHED);
            } else {
                Logger.logErrorMessage("FAILURE! Cannot install critical update.");
                mediator.setUpdateState(UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL);
            }
        } else if (type == TransactionType.Update.IMPORTANT) {
            boolean updated = false;
            while (!updated) {
                updated = scheduleUpdate(updateHeight, attachment, holder.getDecryptedUrl());
                updateHeight = getUpdateHeightFromType(type);
                if (!updated) {
                    Logger.logErrorMessage("Cannot install scheduled important update. Trying to schedule new update attempt at " + updateHeight + " height");
                    mediator.setUpdateHeight(updateHeight);
                    mediator.setUpdateState(UpdateInfo.UpdateState.RE_PLANNING);
                }
            }
            Logger.logInfoMessage("Important update was installed successfully!");
            mediator.setUpdateState(UpdateInfo.UpdateState.FINISHED);
        } else if (type == TransactionType.Update.MINOR) {
            Logger.logInfoMessage("Minor update is available. Required start by user");
            mediator.setUpdateState(UpdateInfo.UpdateState.REQUIRED_START);
        }
    }

    private boolean scheduleUpdate(int updateHeight, Attachment.UpdateAttachment attachment, String decryptedUrl) {
        Logger.logInfoMessage("Update estimated height: ", updateHeight);
        while (mediator.getBlockchainHeight() < updateHeight) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Logger.logErrorMessage("Update thread was awakened");
            }
        }
        Logger.logInfoMessage("Starting scheduled update. CurrentHeight: " + mediator.getBlockchainHeight() + " updateHeight: " + updateHeight);
        return tryUpdate(attachment, decryptedUrl);
    }

    private boolean tryUpdate(Attachment.UpdateAttachment attachment, String decryptedUrl) {
        Logger.logInfoMessage("Update to version: " + attachment.getAppVersion());
        stopForgingAndBlockAcceptance();
        //Downloader downloads update package
        Path path = downloader.tryDownload(decryptedUrl, attachment.getHash());
        if (path != null) {
            if (verifyJar(path)) {
                try {
                    Path unpackedDirPath = unpacker.unpack(path);
                    platformDependentUpdater.continueUpdate(unpackedDirPath, attachment.getPlatform());
                    return true;
                } catch (IOException e) {
                    Logger.logErrorMessage("Cannot unpack file: " + path.toString());
                }
            } else {
                Logger.logErrorMessage("Cannot verify jar signature!");
            }
        }
        return false;
    }

    private void processTransactions(List<? extends Transaction> transactions) {
        transactions.forEach(transaction -> {
            if (mediator.isUpdateTransaction(transaction)) {
                Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
                if (attachment.getAppVersion().greaterThan(mediator.getWalletVersion())) {
                    Platform currentPlatform = Platform.current();
                    Architecture currentArchitecture = Architecture.current();
                    if (attachment.getPlatform() == currentPlatform && attachment.getArchitecture() == currentArchitecture) {
                        String url = tryDecryptUrl(attachment.getUrl(), attachment.getAppVersion());
                        if (url != null && !url.isEmpty()) {
                            if (checker.verifyCertificates(UpdaterConstants.CERTIFICATE_DIRECTORY)) {
                                startUpdate();
                                if (attachment.getLevel() != Level.MINOR) {
                                    mediator.removeListener(updateListener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
                                }
                            } else {
                                Logger.logErrorMessage("Cannot verify certificates!");
                                alertSender.send("Certificate verification error" + transaction.getJSONObject().toJSONString());
                            }
                        } else {
                            Logger.logErrorMessage("Cannot decrypt url for update transaction:" + transaction.getId());
                            alertSender.send("Cannot decrypt url for update transaction:" + transaction.getId());
                        }
                        this.updateDataHolder = new UpdateDataHolder(transaction, url);
                    }
                }
            }
        });
    }

    private boolean verifyJar(Path jarFilePath) {
        try {
            Set<Certificate> certificates = UpdaterUtil.readCertificates(Paths.get(UpdaterConstants.CERTIFICATE_DIRECTORY), UpdaterConstants.CERTIFICATE_SUFFIX, UpdaterConstants.FIRST_DECRYPTION_CERTIFICATE_PREFIX, UpdaterConstants.SECOND_DECRYPTION_CERTIFICATE_PREFIX);
            for (Certificate certificate : certificates) {
                try {
                    checker.verifyJarSignature(certificate, jarFilePath);
                } catch (SecurityException e) {
                    Logger.logWarningMessage("Certificate is not appropriate." + certificate.toString());
                }
                return true;
            }
        } catch (CertificateException | IOException e) {
            Logger.logErrorMessage("Unable to load certificates");
        }
        return false;
    }

    private String tryDecryptUrl(byte[] encryptedUrl, Version updateVersion) {
        Set<CertificatePair> certificatePairs;
        try {
            certificatePairs = UpdaterUtil.buildCertificatePairs(UpdaterConstants.CERTIFICATE_DIRECTORY);
            Cipher cipher = Cipher.getInstance("RSA");
            for (CertificatePair pair : certificatePairs) {
                cipher.init(Cipher.DECRYPT_MODE, pair.getFirstCertificate().getPublicKey());
                byte[] firstDecryptedUrlBytes = cipher.doFinal(encryptedUrl);
                cipher.init(Cipher.DECRYPT_MODE, pair.getSecondCertificate().getPublicKey());
                byte[] secondDecryptedUrlBytes = cipher.doFinal(firstDecryptedUrlBytes);
                String urlString = new String(secondDecryptedUrlBytes, "UTF-8");
                if (urlString.matches(String.format(URL_TEMPLATE, updateVersion.toString()))) {
                    return urlString;
                }
            }
        } catch (IOException | CertificateException e) {
            Logger.logErrorMessage("Cannot read or load certificate", e);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
            Logger.logErrorMessage("Cannot decrypt url", e);
        }
        return null;
    }

    private int getUpdateHeightFromType(TransactionType type) {
        return
                //update is NOW on currentBlockchainHeight
                type == TransactionType.Update.CRITICAL ? mediator.getBlockchainHeight() :

                        // update height = currentBlockchainHeight + random number in range [100.1000]
                        type == TransactionType.Update.IMPORTANT ? new Random().nextInt(900)
                                + 100 + mediator.getBlockchainHeight() :

                                //assume that current update is not mandatory
                                type == TransactionType.Update.MINOR ? -1 : 0;
    }

    private static class UpdaterCoreHolder {
        private static final UpdaterCore HOLDER_INSTANCE = new UpdaterCore();
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
}