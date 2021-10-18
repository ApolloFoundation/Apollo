/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog.EventLogRecord;
import com.apollocurrency.smc.contract.vm.global.Address32;
import com.apollocurrency.smc.data.type.Address;
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
        final Address from = Address32.valueOf("0x010203040506070809");
        final Address to = Address32.valueOf("0x908070605040302010");
        final int amount = 100500;

        var aplEvent = mockAplEvent("{\"amount\":" + amount +
            ",\"from\":{\"class\":\"Address\",\"value\":\"" + from.getHex()
            + "\"},\"to\":{\"class\":\"Address\",\"value\":\"" + to.getHex() + "\"}}");

        var record = EventLogRecord.builder().event(aplEvent).build();
        //WHEN
        manager.emit(eventType, from, to, amount);
        //THEN
        verify(txLog).append(record);
    }
}