/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.udpater.intfce;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.types.update.UpdateTransactionType;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class UpdaterMediatorImpl implements UpdaterMediator {
    private static final Logger LOG = getLogger(UpdaterMediatorImpl.class);
    private final PeersService peers = CDI.current().select(PeersService.class).get();
    private final PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
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
        AplCoreRuntime aplCoreRuntime = CDI.current().select(AplCoreRuntime.class).get();
        if (aplCoreRuntime != null) {
            aplCoreRuntime.shutdown();
        } else {
            LOG.error("Can not shutdown application");
        }
        //       AplCore.removeShutdownHook();
    }

    @Override
    public void suspendBlockchain() {
        lookupBlockchainProcessor().suspendBlockchainDownloading();
        Generator.suspendForging();
        peers.suspend();
    }

    @Override
    public void resumeBlockchain() {
        LOG.debug("Restarting peer server, blockchain processor and forging");
        lookupBlockchainProcessor().resumeBlockchainDownloading();
        peers.resume();
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
    public boolean isUpdateTransaction(Transaction transaction) {
        return UpdateTransactionType.isUpdate(transaction.getType());
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

    @Override
    public PropertiesHolder getPropertyHolder() {
        return propertiesHolder;
    }

    @Override
    public String getChainId() {
        return peers.getMyPeerInfo().getChainId();
    }
}
