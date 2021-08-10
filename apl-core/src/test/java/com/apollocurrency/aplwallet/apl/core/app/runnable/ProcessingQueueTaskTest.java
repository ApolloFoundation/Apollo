/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTxValidationResult;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessingQueueTaskTest {
    @Mock
    UnconfirmedTransactionProcessingService processingService;
    @Mock
    MemPool memPool;
    @Mock
    TransactionValidator validator;
    @Mock
    BatchSizeCalculator batchSizeCalculator;

    ProcessUnconfirmedTransactionsQueueTask task;
    @Mock
    DatabaseManager dbManager;
    @Mock
    TransactionalDataSource dataSource;

    @BeforeEach
    void setUp() {
        doReturn(dataSource).when(dbManager).getDataSource();
        doReturn(mock(TransactionalDataSource.StartedConnection.class)).when(dataSource).beginTransactionIfNotStarted();
        task = new ProcessUnconfirmedTransactionsQueueTask(memPool, validator, processingService, batchSizeCalculator, dbManager);
           }

    @Test
    void processBatchSuccessfully() throws AplException.ValidationException {
        doReturn(20).when(batchSizeCalculator).currentBatchSize();
        doReturn(20).when(memPool).remainingCapacity();
        doAnswer(new Answer<Integer>() {
            int iters = 0;
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) {
                ++iters;
                if (iters == 1) {
                    return 5; // initial size
                }
                if (iters == 6) {
                    return 0;
                }
                return 1;
            }
        }).when(memPool).processingQueueSize();
        UnconfirmedTransaction tx = mock(UnconfirmedTransaction.class);
        doAnswer(new Answer() {
            int i;
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (++i == 3) {
                    throw new AplUnacceptableTransactionValidationException("Test not valid tx", invocationOnMock.getArgument(0));
                }
                return null;
            }
        }).when(validator).validateSufficiently(tx);
        doReturn(tx).when(memPool).nextPendingProcessing();
        doAnswer(new Answer<UnconfirmedTxValidationResult>() {
            int iter;
            @Override
            public UnconfirmedTxValidationResult answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (++iter == 2) {
                    return new UnconfirmedTxValidationResult(1, UnconfirmedTxValidationResult.Error.ALREADY_PROCESSED, "");
                }
                return new UnconfirmedTxValidationResult(0, null, "");
            }
        }).when(processingService).validateBeforeProcessing(tx);
        doAnswer(new Answer() {
            int iter = 0;
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ++iter;
                if (iter == 2) { // not add to mempool second tx
                    return false;
                }
                return true;
            }
        }).when(memPool).addProcessed(tx);
        doAnswer(invocation -> {
            return ((Function) invocation.getArgument(0)).apply(20); // do call on the argument function
        }).when(batchSizeCalculator).doTimedOp(any(Function.class));

        task.processBatch();

        verify(memPool, times(2)).addProcessed(tx);
        verify(batchSizeCalculator).doTimedOp(any(Function.class));
    }

    @Test
    void broadcastBatch_memPool_is_full() {
        doReturn(20).when(batchSizeCalculator).currentBatchSize();
        doReturn(50).when(memPool).processingQueueSize();
        doReturn(0).when(memPool).remainingCapacity();

        task.processBatch();

        verify(memPool, never()).addProcessed(any(UnconfirmedTransaction.class));
    }

    @Test
    void broadcastBatch_memPool_is_empty() {
        doReturn(20).when(batchSizeCalculator).currentBatchSize();
        doReturn(0).when(memPool).processingQueueSize();


        task.processBatch();

        verify(memPool, never()).addProcessed(any(UnconfirmedTransaction.class));
        verify(memPool, never()).remainingCapacity();
    }
}