/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventLogModelToLogEntryConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventLogTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventTable;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractEventServiceImpl;
import com.apollocurrency.aplwallet.apl.smc.events.SmcEventType;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;

import static com.apollocurrency.aplwallet.apl.smc.events.SmcEventBinding.literal;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class SmcContractEventServiceTest extends AbstractContractEventTest {

    @Mock
    Blockchain blockchain;
    @Mock
    SmcContractEventTable contractEventTable;
    @Mock
    SmcContractEventLogTable contractEventLogTable;
    @Mock
    Event<AplContractEvent> cdiEvent;

    ContractEventLogModelToLogEntryConverter logEntryConverter = new ContractEventLogModelToLogEntryConverter();
    ContractEventModelToEntityConverter entityConverter = new ContractEventModelToEntityConverter();

    SmcContractEventService eventService;

    int height = 123;

    @BeforeEach
    void setUp() {
        eventService = new SmcContractEventServiceImpl(blockchain, contractEventTable, contractEventLogTable,
            logEntryConverter, entityConverter,
            hashSumProvider,
            cdiEvent);
    }

    @Test
    void saveEvent() {
        //GIVEN
        long id = 123456789L;
        var dbKey = SmcContractEventTable.KEY_FACTORY.newKey(id);
        var event = createAplEvent(id);
        var entity = entityConverter.convert(event);
        entity.setHeight(height);
        var logEntry = logEntryConverter.convert(event);
        logEntry.setHeight(height);
        when(blockchain.getHeight()).thenReturn(height);
        when(contractEventTable.getDbKeyFactory()).thenReturn(SmcContractEventTable.KEY_FACTORY);

        //WHEN
        eventService.saveEvent(event);

        //THEN
        verify(contractEventTable).get(dbKey);
        verify(contractEventTable).insert(entity);
        verify(contractEventLogTable).insert(logEntry);
    }

    @Test
    void fireEvent() {
        //GIVEN
        long id = 123456789L;
        var event = createAplEvent(id);
        var mockEvent = mock(Event.class);
        when(cdiEvent.select(literal(SmcEventType.EMIT_EVENT))).thenReturn(mockEvent);

        //WHEN
        eventService.fireCdiEvent(event);

        //THEN
        verify(mockEvent).fireAsync(event);

    }
}