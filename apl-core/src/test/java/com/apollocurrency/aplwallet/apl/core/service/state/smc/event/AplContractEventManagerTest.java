/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog.EventLogRecord;
import com.apollocurrency.smc.txlog.TxLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class AplContractEventManagerTest extends AbstractContractEventTest {

    @Mock
    TxLog txLog;

    @BeforeEach
    void setUp() {
        manager = spy(
            new AplContractEventManager(contractAddress, transactionAddress, height, hashSumProvider, txLog)
        );
    }

    @Test
    void emit() {
        //GIVEN
        var aplEvent = mockAplEvent();
        var record = EventLogRecord.builder().event(aplEvent).build();
        //WHEN
        manager.emit(eventType, 1, 2, 3);
        //THEN
        verify(txLog).append(record);
    }
}