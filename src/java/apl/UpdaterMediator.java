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

package apl;

import apl.peer.Peers;
import apl.util.Listener;
import apl.util.Logger;

import java.util.List;
public class UpdaterMediator {
    private final UpdateInfo updateInfo = UpdateInfo.getInstance();

    public UpdateInfo.DownloadStatus getStatus() {return updateInfo.getStatus();}

    public void setStatus(UpdateInfo.DownloadStatus status) {
        Logger.logInfoMessage("Update download status: " + status);
        updateInfo.setStatus(status);
    }

    public UpdateInfo.DownloadState getState() {
        return updateInfo.getState();
    }

    public void setUpdateData(boolean isUpdate, int updateHeight, int receivedUpdateHeight, String updateLevel, Version newVersion) {
        synchronized (updateInfo) {
            updateInfo.setReceivedUpdateHeight(receivedUpdateHeight);
            updateInfo.setUpdate(isUpdate);
            updateInfo.setUpdateVersion(newVersion);
            updateInfo.setUpdateHeight(updateHeight);
            updateInfo.setUpdateLevel(updateLevel);
        }
    }

    public void setState(UpdateInfo.DownloadState state) {
        Logger.logInfoMessage("Update download state: " + state);
        updateInfo.setState(state);
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

    public int getBlockchainHeight() {
        return BlockchainImpl.getInstance().getHeight();
    }
}
