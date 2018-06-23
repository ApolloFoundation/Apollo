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

import apl.Attachment;
import apl.Transaction;
import apl.TransactionType;
import apl.UpdaterMediator;
import apl.util.Listener;
import apl.util.Logger;

import java.util.List;
import java.util.Random;

public class UpdaterCore {
    private static UpdaterCore instance = new UpdaterCore();
    private final UpdaterMediator mediator = UpdaterMediator.getInstance();
    private final SecurityAlertSender alertSender = new SecurityAlertSender();
    private final AuthorityChecker checker = new AuthorityChecker();
    //todo: consider opportunity to move listener to UpdaterMediator
    private final Listener<List<? extends Transaction>> updateListener = transactions ->
            proccessTransactions(transactions);

    private UpdaterCore() {
        mediator.addUpdateListener(updateListener);
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

    public void triggerUpdate(Transaction transaction) {
        TransactionType type = transaction.getType();
        Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
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
        } else if (type == TransactionType.Update.IMPORTANT) {
            //stop forging and peer server at random block (100...1000)
        } else if (type == TransactionType.Update.MINOR) {
            //dont stop forging and peer server. Show user notification, which represents that minor update is available  now
        }
        Logger.logInfoMessage("Starting update to version: " + attachment.getAppVersion());
    }

    public static UpdaterCore getInstance() {
        return instance;
    }

    private void proccessTransactions(List<? extends Transaction> transactions) {
        transactions.forEach(transaction -> {
            if (mediator.isUpdateTransaction(transaction)) {
                Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
                if (!checker.checkSignature(attachment)) {
                    alertSender.send(transaction);
                } else if (attachment.getAppVersion().greaterThan(mediator.getWalletVersion())) {
                    Platform currentPlatform = Platform.current();
                    Architecture currentArchitecture = Architecture.current();
                    if (attachment.getPlatform() == currentPlatform && attachment.getArchitecture() == currentArchitecture) {
                        triggerUpdate(transaction);
                    }
                }
            }
        });
    }

    private int getUpdateHeightFromType(TransactionType type) {
        return type == TransactionType.Update.CRITICAL ?  mediator.getBlockchainHeight() : type == TransactionType.Update.IMPORTANT ? new Random().nextInt(900) + 100 + mediator.getBlockchainHeight() :
            type == TransactionType.Update.MINOR ? -1 : 0;//assume not mandatory update
    }

}