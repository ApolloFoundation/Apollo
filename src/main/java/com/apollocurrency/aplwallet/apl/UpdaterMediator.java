/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.peer.Peers;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Logger;

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

    public void setUpdateState(UpdateInfo.UpdateState updateState) {

        Logger.logInfoMessage("Update state: " + updateState);
        updateInfo.setUpdateState(updateState);}

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
        private static final UpdaterMediator INSTANCE = new UpdaterMediator();
    }

    public static UpdaterMediator getInstance() {
        return UpdaterMediatorHolder.INSTANCE;
    }
    private UpdaterMediator() {}


    public void stopForging() {
        Generator.suspendForging();
    }

    public void shutdownPeerServer() {
        Peers.suspend();
    }

    public void shutdownBlockchainProcessor() {
        BlockchainProcessorImpl.getInstance().suspendBlockchainDownloading();
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

    public void restoreConnection() {
        Logger.logDebugMessage("Restarting peer server, blockchain processor and forging");
        BlockchainProcessorImpl.getInstance().resumeBlockchainDownloading();
        Peers.resume();
        Generator.resumeForging();
        Logger.logDebugMessage("Peer server, blockchain processor and forging were restarted successfully");
    }
}
