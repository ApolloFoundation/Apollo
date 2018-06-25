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

import java.util.List;
//todo consider UpdaterMediator interface with only get methods
public class UpdaterMediator {
    private boolean isUpdate = false;
    private long updateHeight;
    private int receivedUpdateHeight;
    private String updateLevel = "";
    private Version updateVersion;

    public synchronized Version getUpdateVersion() {
        return updateVersion;
    }

    public void setUpdateVersion(Version updateVersion) {
        this.updateVersion = updateVersion;
    }

    private static UpdaterMediator instance = new UpdaterMediator();

    private UpdaterMediator() {}

    public synchronized long getUpdateHeight() {
        return updateHeight;
    }

    public void setUpdateHeight(long updateHeight) {
        this.updateHeight = updateHeight;
    }

    public synchronized int getReceivedUpdateHeight() {
        return receivedUpdateHeight;
    }

    public void setReceivedUpdateHeight(int receivedUpdateHeight) {
        this.receivedUpdateHeight = receivedUpdateHeight;
    }

    public synchronized String getUpdateLevel() {

        return updateLevel;
    }

    public void setUpdateLevel(String updateLevel) {
        this.updateLevel = updateLevel;
    }

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

    public synchronized boolean isUpdate() {
        return isUpdate;
    }

    public void setUpdate(boolean update) {
        isUpdate = update;
    }

    public static UpdaterMediator getInstance() {
        return instance;
    }

    public static void setInstance(UpdaterMediator instance) {
        UpdaterMediator.instance = instance;
    }

    public int getBlockchainHeight() {
        return BlockchainImpl.getInstance().getHeight();
    }
    public static class UpdateInfo {
        private boolean isUpdate;
        private long updateHeight;
        private int receivedUpdateHeight;
        private String updateLevel;
        private Version updateVersion;

        public boolean isUpdate() {
            return isUpdate;
        }

        private UpdateInfo(boolean isUpdate, long updateHeight, int receivedUpdateHeight, String updateLevel, Version updateVersion) {
            this.isUpdate = isUpdate;
            this.updateHeight = updateHeight;
            this.receivedUpdateHeight = receivedUpdateHeight;
            this.updateLevel = updateLevel;
            this.updateVersion = updateVersion;
        }

        public void setUpdate(boolean update) {
            isUpdate = update;
        }

        public long getUpdateHeight() {
            return updateHeight;
        }

        public int getReceivedUpdateHeight() {
            return receivedUpdateHeight;
        }

        public String getUpdateLevel() {
            return updateLevel;
        }


        public Version getUpdateVersion() {
            return updateVersion;
        }
    }

    public synchronized UpdateInfo getUpdateInfo() {
        return new UpdateInfo(isUpdate(), getUpdateHeight(), getReceivedUpdateHeight(), getUpdateLevel(), getUpdateVersion());
    }
}
