/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl;

import apl.peer.Peers;
import apl.util.Listener;
import apl.util.Logger;

import java.util.List;
public class UpdaterMediator {
    private final UpdateInfo updateInfo = UpdateInfo.getInstance();

    public UpdateInfo.DownloadStatus getStatus() {return updateInfo.getDownloadStatus();}

    public void setStatus(UpdateInfo.DownloadStatus status) {
        Logger.logInfoMessage("Update download status: " + status);
        updateInfo.setDownloadStatus(status);
    }

    public UpdateInfo.DownloadState getState() {
        return updateInfo.getDownloadState();
    }

    public UpdateInfo.UpdateState getUpdateState() {return updateInfo.getUpdateState();}

    public void setUpdateState(UpdateInfo.UpdateState updateState) {updateInfo.setUpdateState(updateState);}

    public void setUpdateData(boolean isUpdate, int updateHeight, int receivedUpdateHeight, Level updateLevel, Version newVersion) {
        //required
        synchronized (updateInfo) {
            updateInfo.setReceivedHeight(receivedUpdateHeight);
            updateInfo.setUpdate(isUpdate);
            updateInfo.setVersion(newVersion);
            updateInfo.setEstimatedHeight(updateHeight);
            updateInfo.setLevel(updateLevel);
        }
    }

    public void setUpdateHeight(int updateHeight) {updateInfo.setEstimatedHeight(updateHeight);}

    public void setState(UpdateInfo.DownloadState state) {
        Logger.logInfoMessage("Update download state: " + state);
        updateInfo.setDownloadState(state);
    }
    public void shutdownApplication() {
        Apl.shutdown();
    }

    public boolean isUpdate() {return updateInfo.isUpdate();}

    public void setUpdate(boolean update) {updateInfo.setUpdate(update);}


    private static class UpdaterMediatorHolder {
        private static final UpdaterMediator HOLDER_INSTANCE = new UpdaterMediator();
    }

    public static UpdaterMediator getInstance() {
        return UpdaterMediatorHolder.HOLDER_INSTANCE;
    }
    private UpdaterMediator() {}


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

    public boolean isShutdown() {
        return Apl.isShutdown();
    }

    public int getBlockchainHeight() {
        return BlockchainImpl.getInstance().getHeight();
    }
}
