/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
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

    PendingBroadcastTask pendingBroadcastTask;

    @BeforeEach
    void setUp() {
        pendingBroadcastTask = new PendingBroadcastTask(transactionProcessor, memPool, validator, processingService);
    }

    @Test
    void broadcastPendingQueue() {

    }

    @Test
    void batchSize() {
        doReturn(0.2).when(memPool).pendingBroadcastQueueLoad();
        int size = pendingBroadcastTask.batchSize();
        assertEquals(654, size);
    }

    @Test
    void nextValidTxFromPendingQueue() {

    }
}