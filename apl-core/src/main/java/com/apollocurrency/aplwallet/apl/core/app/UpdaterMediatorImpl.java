/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.ConnectionProvider;
import com.apollocurrency.aplwallet.apl.util.Listener;
import org.slf4j.Logger;

@ApplicationScoped
public class UpdaterMediatorImpl implements UpdaterMediator {
    private static final Logger LOG = getLogger(UpdaterMediatorImpl.class);

    private TransactionDb transactionDb;

    @Inject
    public UpdaterMediatorImpl(TransactionDb transactionDb) {
        this.transactionDb = transactionDb;
    }

    @Override
    public void shutdownApplication() {
        AplCoreRuntime.getInstance().shutdown();
 //       AplCore.removeShutdownHook();
    }

    @Override
    public void suspendBlockchain() {
        BlockchainProcessorImpl.getInstance().suspendBlockchainDownloading();
        Generator.suspendForging();
        Peers.suspend();
    }

    @Override
    public void resumeBlockchain() {
        LOG.debug("Restarting peer server, blockchain processor and forging");
        BlockchainProcessorImpl.getInstance().resumeBlockchainDownloading();
        Peers.resume();
        Generator.resumeForging();
        LOG.debug("Peer server, blockchain processor and forging were restarted successfully");
    }

    @Override
    public void addUpdateListener(Listener<List<? extends Transaction>> listener) {
        TransactionProcessorImpl.getInstance().addListener(listener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }

    @Override
    public void removeUpdateListener(Listener<List<? extends Transaction>> listener) {
        TransactionProcessorImpl.getInstance().removeListener(listener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }

    @Override
    public boolean isUpdateTransaction(Transaction transaction) {
        return TransactionType.Update.isUpdate(transaction.getType());
    }

    @Override
    public Version getWalletVersion() {
        return Constants.VERSION;
    }

    @Override
    public boolean isShutdown() {
        return AplCore.isShutdown();
    }

    @Override
    public ConnectionProvider getConnectionProvider() {
        return new ConnectionProviderImpl();
    }

    @Override
    public int getBlockchainHeight() {
        return BlockchainImpl.getInstance().getHeight();
    }

    @Override
    public Transaction loadTransaction(Connection connection, ResultSet rs) throws AplException.NotValidException {
        return transactionDb.loadTransaction(connection, rs);
    }
}
