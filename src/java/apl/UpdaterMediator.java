package apl;

import apl.peer.Peers;
import apl.util.Listener;

import java.util.List;

public class UpdaterMediator {

    public UpdaterMediator() {}

    public int stopForging() {
        return Generator.stopForging();
    }

    public void shutdownPeerServer() {
        Peers.shutdown();
    }

    public void shutdownBlockchainProcessor() {
        BlockchainProcessorImpl.getInstance().shutdown();
    }

    public void addUpdateListener(Listener<List<? extends Transaction>> listener) {
        TransactionProcessorImpl.getInstance().addListener(listener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }

    public void removeListener(Listener<List<? extends Transaction>> listener, TransactionProcessor.Event eventType) {
        TransactionProcessorImpl.getInstance().removeListener(listener, eventType);
    }
    public boolean isUpdateTransaction(Transaction transaction) {
        return TransactionType.Update.isUpdate(transaction.getType());
    }

    public Version getWalletVersion() {
        return Apl.VERSION;
    }

}
