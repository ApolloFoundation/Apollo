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

public class UpdaterCore {
    private final UpdaterMediator mediator = UpdaterMediator.getInstance();
    private final Downloader downloader = Downloader.getInstance();
    private final SecurityAlertSender alertSender = new SecurityAlertSender();
    private final AuthorityChecker checker = new AuthorityChecker();
    private final Unpacker unpacker = new Unpacker();
    private final PlatformDependentUpdater platformDependentUpdater = new PlatformDependentUpdater();

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

    public void triggerUpdate(Transaction transaction) {
        TransactionType type = transaction.getType();
        Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
        //todo: consider separating mediator and update status holder
        synchronized (mediator) {
            mediator.setReceivedUpdateHeight(transaction.getHeight());
            mediator.setUpdate(true);
            mediator.setUpdateLevel(type.getName());
            mediator.setUpdateVersion(attachment.getAppVersion());
            mediator.setUpdateHeight(getUpdateHeightFromType(type));
        }
        if (type == TransactionType.Update.CRITICAL) {
            //stop forging and peer server immediately
            stopForgingAndBlockAcceptance();
            //Downloader downloads update package
            Path path = downloader.tryDownload(attachment.getUrl(), attachment.getHash());
            if (path != null) {
                try {
                    Path unpackedDirPath = unpacker.unpack(path);
                    platformDependentUpdater.continueUpdate(unpackedDirPath);
                }
                catch (IOException e) {
                    Logger.logErrorMessage("Cannot unpack file: " + path.toString());
                }
            } else {
                Logger.logErrorMessage("FAILURE! Update file was not downloaded");
                //some important actions (notify user, update state, etc.)
            }
        } else if (type == TransactionType.Update.IMPORTANT) {
            //stop forging and peer server at random block (100...1000)
        } else if (type == TransactionType.Update.MINOR) {
            //dont stop forging and peer server. Show user notification, which represents that minor update is available  now
        }
        Logger.logInfoMessage("Starting update to version: " + attachment.getAppVersion());
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
                            new Thread(() -> triggerUpdate(transaction), "Apollo updater thread").start();
                            mediator.removeListener(updateListener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
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