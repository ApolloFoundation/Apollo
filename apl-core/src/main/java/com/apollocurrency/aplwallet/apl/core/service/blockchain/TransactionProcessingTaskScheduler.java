/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessLaterTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessTxsToBroadcastWhenConfirmed;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessUnconfirmedTransactionsQueueTask;
import com.apollocurrency.aplwallet.apl.core.app.runnable.RebroadcastTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.RemoveUnconfirmedTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TransactionProcessingTaskScheduler {
    private final TimeService timeService;
    private final Blockchain blockchain;
    private final MemPool memPool;
    private final PeersService peersService;
    private final TransactionProcessor transactionProcessor;
    private final BlockchainConfig blockchainConfig;
    private final TransactionBuilderFactory builderFactory;
    private final DatabaseManager databaseManager;
    private final PropertiesHolder propertiesHolder;
    private final TaskDispatchManager taskDispatchManager;
    private final TransactionValidator transactionValidator;
    private final UnconfirmedTransactionProcessingService processingService;
    private final UnconfirmedTransactionCreator unconfirmedTransactionCreator;
    private final BatchSizeCalculator batchSizeCalculator;

    @Inject
    public TransactionProcessingTaskScheduler(PropertiesHolder propertiesHolder, TimeService timeService,
                                              Blockchain blockchain, MemPool memPool, PeersService peersService,
                                              TransactionProcessor transactionProcessor, BlockchainConfig blockchainConfig,
                                              TransactionBuilderFactory builderFactory, DatabaseManager databaseManager,
                                              TaskDispatchManager taskDispatchManager, TransactionValidator transactionValidator,
                                              UnconfirmedTransactionProcessingService processingService,
                                              BatchSizeCalculator batchSizeCalculator,
                                              UnconfirmedTransactionCreator unconfirmedTransactionCreator) {
        this.timeService = timeService;
        this.blockchain = blockchain;
        this.memPool = memPool;
        this.peersService = peersService;
        this.transactionProcessor = transactionProcessor;
        this.blockchainConfig = blockchainConfig;
        this.builderFactory = builderFactory;
        this.databaseManager = databaseManager;
        this.propertiesHolder = propertiesHolder;
        this.taskDispatchManager = taskDispatchManager;
        this.transactionValidator = transactionValidator;
        this.processingService = processingService;
        this.batchSizeCalculator = batchSizeCalculator;
        this.unconfirmedTransactionCreator = unconfirmedTransactionCreator;
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
                        builderFactory))
                    .build());

                dispatcher.invokeAfter(Task.builder()
                    .name("InitialUnconfirmedTxsRebroadcasting")
                    .task(transactionProcessor::rebroadcastAllUnconfirmedTransactions)
                    .build());

                dispatcher.schedule(Task.builder()
                    .name("RebroadcastTransactions")
                    .delay(8000)
                    .task(new RebroadcastTransactionsThread(
                        this.timeService, memPool, peersService, this.blockchain, unconfirmedTransactionCreator))
                    .build());
            }

            dispatcher.schedule(Task.builder()
                .name("RemoveUnconfirmedTransactions")
                .delay(5000)
                .task(new RemoveUnconfirmedTransactionsThread(
                    this.databaseManager, transactionProcessor, this.timeService, memPool))
                .build());

            dispatcher.schedule(Task.builder()
                .name("ProcessLaterTransactions")
                .delay(3000)
                .task(new ProcessLaterTransactionsThread(transactionProcessor))
                .build());

            dispatcher.schedule(Task.builder()
                .name("ProcessTransactionsToBroadcastWhenConfirmed")
                .delay(15000)
                .task(new ProcessTxsToBroadcastWhenConfirmed(transactionProcessor,
                    memPool, this.timeService, this.blockchain))
                .build());

            dispatcher.schedule(Task.builder()
                .name("ProcessUnconfirmedTransactionsQueue")
                .delay(500)
                .task(new ProcessUnconfirmedTransactionsQueueTask(
                    memPool,transactionValidator, processingService,batchSizeCalculator, databaseManager))
                .build());
        }
    }
}
