/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionHelper;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class UnconfirmedTransactionProcessingService {
    private final TimeService timeService;
    private final Blockchain blockchain;
    private final BlockchainConfig blockchainConfig;
    private final MemPool memPool;
    private final TransactionValidator validator;
    private final AccountService accountService;
    private final TransactionApplier transactionApplier;
    private final DatabaseManager databaseManager;

    @Inject
    public UnconfirmedTransactionProcessingService(TimeService timeService, Blockchain blockchain, BlockchainConfig blockchainConfig, MemPool memPool, TransactionValidator validator, AccountService accountService, TransactionApplier transactionApplier, DatabaseManager databaseManager) {
        this.timeService = timeService;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
        this.memPool = memPool;
        this.validator = validator;
        this.accountService = accountService;
        this.transactionApplier = transactionApplier;
        this.databaseManager = databaseManager;
    }


    public UnconfirmedTxValidationResult validateBeforeProcessing(Transaction transaction) {
        int curTime = timeService.getEpochTime();
        if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT || transaction.getExpiration() < curTime) {
            return new UnconfirmedTxValidationResult(100_100, UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID, "Invalid transaction timestamp");
        }
        if (transaction.getVersion() < 1) {
            return new UnconfirmedTxValidationResult(100_105, UnconfirmedTxValidationResult.Error.NOT_VALID, "Invalid transaction version");
        }

        if (transaction.getId() == 0L) {
            return new UnconfirmedTxValidationResult(100_110, UnconfirmedTxValidationResult.Error.NOT_VALID, "Invalid transaction id 0");
        }
        if (blockchain.getHeight() < blockchainConfig.getLastKnownBlock()) {
            return new UnconfirmedTxValidationResult(100_115, UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID, "Blockchain not ready to accept transactions");
        }
        if (memPool.getUnconfirmedTransaction(transaction.getId()) != null || blockchain.hasTransaction(transaction.getId())) {
            return new UnconfirmedTxValidationResult(100_120, UnconfirmedTxValidationResult.Error.ALREADY_PROCESSED, "Transaction already processed");
        }

        if (!validator.verifySignature(transaction)) {
            if (accountService.getAccount(transaction.getSenderId()) != null) {
                return new UnconfirmedTxValidationResult(100_125, UnconfirmedTxValidationResult.Error.NOT_VALID, "Transaction signature verification failed");
            } else {
                return new UnconfirmedTxValidationResult(100_130, UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID, "Unknown transaction sender");
            }
        }
        return new UnconfirmedTxValidationResult(0, null, "");
    }

    public boolean addNewUnconfirmedTransaction(UnconfirmedTransaction unconfirmedTransaction) {
        return TransactionHelper.executeInTransaction(databaseManager.getDataSource(), ()-> {
            unconfirmedTransaction.setHeight(blockchain.getHeight());
            return memPool.addProcessed(unconfirmedTransaction);
        });
    }

//    public void processTransaction(UnconfirmedTransaction unconfirmedTransaction) {
//        Transaction transaction = unconfirmedTransaction.getTransaction();
//        TransactionalDataSource dataSource = databaseManager.getDataSource();
//        TransactionHelper.executeInTransaction(dataSource, () -> {
//                if (!transactionApplier.applyUnconfirmed(transaction)) {
//                    throw new AplException.InsufficientBalanceException("Insufficient balance");
//                }
////                if (memPool.isUnconfirmedDuplicate(transaction)) {
////                    throw new AplException.NotCurrentlyValidException("Duplicate unconfirmed transaction");
////                }
//                unconfirmedTransaction.setHeight(blockchain.getHeight());
//                memPool.addProcessed(unconfirmedTransaction);
//            }
//        );
//    }

//    public List<UnconfirmedTransaction> undoAllProcessedTransactions() {
//        List<UnconfirmedTransaction> removed = new ArrayList<>();
//        memPool.getAllProcessedStream().forEach(e -> {
//            transactionApplier.undoUnconfirmed(e.getTransaction());
//            removed.add(e);
//        });
//        return removed;
//    }

//    public void undoProcessedTransaction(Transaction transaction) {
//        transactionApplier.undoUnconfirmed(transaction);
//    }
}
