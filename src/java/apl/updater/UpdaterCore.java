/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.updater;

import apl.*;
import apl.util.Listener;
import apl.util.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class UpdaterCore {
    private final UpdaterMediator mediator = UpdaterMediator.getInstance();
    private final Downloader downloader = Downloader.getInstance();
    private final SecurityAlertSender alertSender = new SecurityAlertSender();
    private final AuthorityChecker checker = new AuthorityChecker();
    private final Unpacker unpacker = new Unpacker();
    private final PlatformDependentUpdater platformDependentUpdater = new PlatformDependentUpdater();
    private Transaction updateTransaction;
    //todo: consider opportunity to move listener to UpdaterMediator
    private final Listener<List<? extends Transaction>> updateListener = this::processTransactions;

    private UpdaterCore() {
        mediator.addUpdateListener(updateListener);
    }

    public static UpdaterCore getInstance() {
        return UpdaterCoreHolder.HOLDER_INSTANCE;
    }

    //todo: consider using separated Logger
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
        new Thread(()-> triggerUpdate(updateTransaction), "Apollo updater thread").start();
    }

    public void triggerUpdate(Transaction updateTransaction) {
        TransactionType type = updateTransaction.getType();
        Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) updateTransaction.getAttachment();
        int updateHeight = getUpdateHeightFromType(type);
        mediator.setUpdateData(true, updateHeight, updateTransaction.getHeight(), attachment.getLevel(), attachment.getAppVersion());
        mediator.setUpdateState(UpdateInfo.UpdateState.IN_PROGRESS);
        if (type == TransactionType.Update.CRITICAL) {
            //stop forging and peer server immediately
            Logger.logWarningMessage("Starting critical update now!");
            if (tryUpdate(attachment)) {
                Logger.logInfoMessage("Critical update was successfully installed");
                mediator.setUpdateState(UpdateInfo.UpdateState.FINISHED);
            } else {
                Logger.logErrorMessage("FAILURE! Cannot install critical update.");
                mediator.setUpdateState(UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL);
            }
        } else if (type == TransactionType.Update.IMPORTANT) {
            boolean updated = false;
            while (!updated) {
                updated = scheduleUpdate(updateHeight, attachment);
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

    private boolean scheduleUpdate(int updateHeight, Attachment.UpdateAttachment attachment) {
        Logger.logInfoMessage("Update estimated height: ", updateHeight);
        while (mediator.getBlockchainHeight() < updateHeight) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            }
            catch (InterruptedException e) {
                Logger.logErrorMessage("Update thread was awakened");
            }
        }
        Logger.logInfoMessage("Starting scheduled update. CurrentHeight: " + mediator.getBlockchainHeight() + " updateHeight: " + updateHeight);
        return tryUpdate(attachment);
              }

    private boolean tryUpdate(Attachment.UpdateAttachment attachment) {
        Logger.logInfoMessage("Update to version: " + attachment.getAppVersion());
        stopForgingAndBlockAcceptance();
        //Downloader downloads update package
        Path path = downloader.tryDownload(attachment.getUrl(), attachment.getHash());
        if (path != null) {
            try {
                Path unpackedDirPath = unpacker.unpack(path);
                platformDependentUpdater.continueUpdate(unpackedDirPath, attachment.getPlatform());
                return true;
            }
            catch (IOException e) {
                Logger.logErrorMessage("Cannot unpack file: " + path.toString());
            }
        }
        return false;
    }

    private void processTransactions(List<? extends Transaction> transactions) {
        transactions.forEach(transaction -> {
            if (mediator.isUpdateTransaction(transaction)) {
                Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
                if (attachment.getAppVersion().greaterThan(mediator.getWalletVersion())) {
                    if (!checker.checkSignature(attachment)) {
                        alertSender.send(transaction);
                    } else {
                        Platform currentPlatform = Platform.current();
                        Architecture currentArchitecture = Architecture.current();
                        if (attachment.getPlatform() == currentPlatform && attachment.getArchitecture() == currentArchitecture) {
                            this.updateTransaction = transaction;
                            startUpdate();
                            if (attachment.getLevel() != Level.MINOR) {
                                mediator.removeListener(updateListener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
                            }
                        }
                    }
                }
            }
        });
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

}