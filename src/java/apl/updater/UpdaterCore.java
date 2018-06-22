package apl.updater;

import apl.Attachment;
import apl.Transaction;
import apl.UpdaterMediator;
import apl.util.Listener;
import apl.util.Logger;

import java.util.List;

public class UpdaterCore {
    private static UpdaterCore instance = new UpdaterCore();
    private final UpdaterMediator mediator = new UpdaterMediator();
    private final SecurityAlertSender alertSender = new SecurityAlertSender();
    private final AuthorityChecker checker = new AuthorityChecker();
    //todo: consider opportunity to move listener to UpdaterMediator
    private final Listener<List<? extends Transaction>> updateListener = transactions -> {
        transactions.forEach(transaction -> {
            if (mediator.isUpdateTransaction(transaction)) {
                Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
                if (!checker.checkSignature(attachment)) {
                    alertSender.send(transaction);
                } else if (attachment.getAppVersion().greaterThan(mediator.getWalletVersion())) {
                    Platform currentPlatform = Platform.current();
                    Architecture currentArchitecture = Architecture.current();
                    if (attachment.getPlatform() == currentPlatform && attachment.getArchitecture() == currentArchitecture) {
                        triggerUpdate(attachment);
                    }
                }
            }
        });
    };

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

    public void triggerUpdate(Attachment.UpdateAttachment attachment) {
        Logger.logInfoMessage("Starting update to version: " +  attachment.getAppVersion());
    }

    public static UpdaterCore getInstance() {
        return instance;
    }
}
