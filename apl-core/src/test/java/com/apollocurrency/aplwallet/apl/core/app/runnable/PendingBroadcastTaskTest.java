/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTxValidationResult;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PendingBroadcastTaskTest {
    @Mock
    UnconfirmedTransactionProcessingService processingService;
    @Mock
    TransactionProcessor transactionProcessor;
    @Mock
    MemPool memPool;
    @Mock
    TransactionValidator validator;
    @Mock
    BatchSizeCalculator batchSizeCalculator;

    PendingBroadcastTask pendingBroadcastTask;

    @BeforeEach
    void setUp() {
        pendingBroadcastTask = new PendingBroadcastTask(transactionProcessor, memPool, batchSizeCalculator, validator, processingService);
    }

    @Test
    void broadcastBatchSuccessfully() throws InterruptedException, AplException.ValidationException {
        doReturn(20).when(batchSizeCalculator).currentBatchSize();
        doReturn(20).when(memPool).canSafelyAccept();
        doAnswer(new Answer<Integer>() {
            int iters = 0;
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                ++iters;
                if (iters == 1) {
                    return 5; // initial size
                }
                if (iters == 7) {
                    return 0;
                }
                return 1;
            }
        }).when(memPool).pendingBroadcastQueueSize();
        Transaction tx = mock(Transaction.class);
        doAnswer(new Answer() {
            int i;
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (++i == 4) {
                    throw new AplException.NotValidException("Test not valid tx");
                }
                return null;
            }
        }).when(validator).validate(tx);
        doReturn(tx).when(memPool).nextSoftBroadcastTransaction();
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

        pendingBroadcastTask.broadcastBatch();

        verify(transactionProcessor).broadcast(List.of(tx, tx, tx));

    }

    @Test
    void broadcastBatch_memPool_is_full() {
        doReturn(20).when(batchSizeCalculator).currentBatchSize();
        doReturn(0).when(memPool).canSafelyAccept();
        pendingBroadcastTask.broadcastBatch();

        verify(transactionProcessor, never()).broadcast(any(List.class));
    }
}