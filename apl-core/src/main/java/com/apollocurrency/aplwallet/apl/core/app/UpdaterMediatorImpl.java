/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.Constants;
import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Listener;
import org.slf4j.Logger;

@Singleton
public class UpdaterMediatorImpl implements UpdaterMediator {
    private static final Logger LOG = getLogger(UpdaterMediatorImpl.class);

    private TransactionProcessor transactionProcessor;
    private BlockchainProcessor blockchainProcessor;
    private Blockchain blockchain;

//    @Inject
/*
    public UpdaterMediatorImpl(Blockchain blockchain) {
        this.blockchain = blockchain;
    }
*/
    public UpdaterMediatorImpl() {
    }

    @Override
    public void shutdownApplication() {
        AplCoreRuntime.getInstance().shutdown();
 //       AplCore.removeShutdownHook();
    }

    @Override
    public void suspendBlockchain() {
        lookupBlockchainProcessor().suspendBlockchainDownloading();
        Generator.suspendForging();
        Peers.suspend();
    }

    @Override
    public void resumeBlockchain() {
        LOG.debug("Restarting peer server, blockchain processor and forging");
        lookupBlockchainProcessor().resumeBlockchainDownloading();
        Peers.resume();
        Generator.resumeForging();
        LOG.debug("Peer server, blockchain processor and forging were restarted successfully");
    }

    private TransactionProcessor lookupTransactionProcessor() {
        if (transactionProcessor == null) {
            transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
        }
        return transactionProcessor;
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        }
        return blockchainProcessor;
    }

    private Blockchain lookupBlockchain() {
        if (blockchain == null) {
            blockchain = CDI.current().select(BlockchainImpl.class).get();
        }
        return blockchain;
    }

    @Override
    public void addUpdateListener(Listener<List<? extends Transaction>> listener) {
        lookupTransactionProcessor().addListener(listener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }

    @Override
    public void removeUpdateListener(Listener<List<? extends Transaction>> listener) {
        lookupTransactionProcessor().removeListener(listener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }

    @Override
    public boolean isUpdateTransaction(Transaction transaction) {
        return Update.isUpdate(transaction.getType());
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
    public TransactionalDataSource getDataSource() {
        DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();
        return databaseManager.getDataSource();
    }

    @Override
    public int getBlockchainHeight() {
        return lookupBlockchain().getHeight();
    }

    @Override
    public Transaction loadTransaction(Connection connection, ResultSet rs) throws AplException.NotValidException {
        return lookupBlockchain().loadTransaction(connection, rs);
    }
}
