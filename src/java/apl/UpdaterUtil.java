package apl;

import apl.peer.Peers;
import apl.util.Logger;

public class UpdaterUtil {
    public static void stopForgingAndBlockAcceptance() {
        Logger.logDebugMessage("Stopping forging...");
        int numberOfGenerators = Generator.stopForging();
        Logger.logInfoMessage("Forging was stopped, total generators: " + numberOfGenerators);
        Logger.logDebugMessage("Shutdown peer server...");
        Peers.shutdown();
        Logger.logInfoMessage("Peer server was shutdown");
        Logger.logDebugMessage("Shutdown blockchain processor...");
        BlockchainProcessorImpl.getInstance().shutdown();
        Logger.logInfoMessage("Blockchain processor was shutdown");
    }
}
