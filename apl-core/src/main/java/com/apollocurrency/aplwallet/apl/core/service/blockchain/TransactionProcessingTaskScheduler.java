/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.runnable.PendingBroadcastTask;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessTxsToBroadcastWhenConfirmed;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessLaterTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.RebroadcastTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.RemoveUnconfirmedTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionProcessingTaskScheduler {
    private final TimeService timeService;
    private final Blockchain blockchain;
    private final MemPool memPool;
    private final PeersService peersService;
    private final GlobalSync globalSync;
    private final TransactionProcessor transactionProcessor;
    private final BlockchainConfig blockchainConfig;
    private final TransactionTypeFactory transactionTypeFactory;
    private final DatabaseManager databaseManager;
    private final PropertiesHolder propertiesHolder;
    private final TaskDispatchManager taskDispatchManager;
    private final TransactionValidator transactionValidator;
    private final UnconfirmedTransactionProcessingService processingService;
    private final BatchSizeCalculator batchSizeCalculator;

    @Inject
    public TransactionProcessingTaskScheduler(PropertiesHolder propertiesHolder, TimeService timeService, Blockchain blockchain, MemPool memPool, PeersService peersService, GlobalSync globalSync, TransactionProcessor transactionProcessor, BlockchainConfig blockchainConfig, TransactionTypeFactory transactionTypeFactory, DatabaseManager databaseManager, TaskDispatchManager taskDispatchManager, TransactionValidator transactionValidator, UnconfirmedTransactionProcessingService processingService, BatchSizeCalculator batchSizeCalculator) {
        this.timeService = timeService;
        this.blockchain = blockchain;
        this.memPool = memPool;
        this.peersService = peersService;
        this.globalSync = globalSync;
        this.transactionProcessor = transactionProcessor;
        this.blockchainConfig = blockchainConfig;
        this.transactionTypeFactory = transactionTypeFactory;
        this.databaseManager = databaseManager;
        this.propertiesHolder = propertiesHolder;
        this.taskDispatchManager = taskDispatchManager;
        this.transactionValidator = transactionValidator;
        this.processingService = processingService;
        this.batchSizeCalculator = batchSizeCalculator;
    }

    @PostConstruct
    public void init() {
        configureBackgroundTasks();
    }

    private void configureBackgroundTasks() {
        if (!propertiesHolder.isLightClient()) {
            TaskDispatcher dispatcher = taskDispatchManager.newBackgroundDispatcher("TransactionProcessorService");
            if (!propertiesHolder.isOffline()) {
                dispatcher.schedule(Task.builder()
                    .name("ProcessTransactions")
                    .delay(1000)
                    .task(new ProcessTransactionsThread(
                        transactionProcessor, memPool, blockchainConfig, peersService,
                        transactionTypeFactory))
                    .build());
                dispatcher.schedule(Task.builder()
                    .name("MemPoolChecker")
                    .delay(1000)
                    .task(transactionProcessor::printMemPoolStat)
                    .build());
                dispatcher.schedule(Task.builder()
                    .name("PendingBroadcaster")
                    .delay(100)
                    .task(new PendingBroadcastTask( transactionProcessor,  memPool, batchSizeCalculator, transactionValidator, processingService))
                    .build());
                dispatcher.invokeAfter(Task.builder()
                    .name("InitialUnconfirmedTxsRebroadcasting")
                    .task(transactionProcessor::rebroadcastAllUnconfirmedTransactions)
                    .build());

                dispatcher.schedule(Task.builder()
                    .name("RebroadcastTransactions")
                    .delay(8000)
                    .task(new RebroadcastTransactionsThread(
                        this.timeService, memPool, peersService, this.blockchain))
                    .build());
            }
            dispatcher.schedule(Task.builder()
                .name("RemoveUnconfirmedTransactions")
                .delay(7000)
                .task(new RemoveUnconfirmedTransactionsThread(
                    this.databaseManager, transactionProcessor, this.timeService, memPool, this.globalSync))
                .build());
            dispatcher.schedule(Task.builder()
                .name("ProcessWaitingTransactions")
                .delay(3000)
                .task(new ProcessLaterTransactionsThread(transactionProcessor))
                .build());
            dispatcher.schedule(Task.builder()
                .name("ProcessTransactionsToBroadcastWhenConfirmed")
                .delay(15000)
                .task(new ProcessTxsToBroadcastWhenConfirmed(
                    memPool, this.timeService, this.blockchain))
                .build());
        }
    }
}
