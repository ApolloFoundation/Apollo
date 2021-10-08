/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventLogModelToLogEntryConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventLogTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventTable;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.smc.events.SmcEventType;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.apollocurrency.aplwallet.apl.smc.events.SmcEventBinding.literal;
import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractEventServiceImpl implements SmcContractEventService {

    private final Blockchain blockchain;
    private final SmcContractEventTable contractEventTable;
    private final SmcContractEventLogTable contractEventLogTable;
    private final ContractEventLogModelToLogEntryConverter logEntryConverter;
    private final ContractEventModelToEntityConverter entityConverter;
    private final HashSumProvider hashSumProvider;
    private final Event<AplContractEvent> cdiContractEvent;

    @Inject
    public SmcContractEventServiceImpl(Blockchain blockchain, SmcContractEventTable contractEventTable, SmcContractEventLogTable contractEventLogTable, ContractEventLogModelToLogEntryConverter contractEventLogModelToLogEntryConverter, ContractEventModelToEntityConverter contractEventModelToEntityConverter, HashSumProvider hashSumProvider, Event<AplContractEvent> cdiContractEvent) {
        this.blockchain = blockchain;
        this.contractEventTable = contractEventTable;
        this.contractEventLogTable = contractEventLogTable;
        this.logEntryConverter = contractEventLogModelToLogEntryConverter;
        this.entityConverter = contractEventModelToEntityConverter;
        this.hashSumProvider = hashSumProvider;
        this.cdiContractEvent = cdiContractEvent;
    }

    @Override
    @Transactional
    public void saveEvent(AplContractEvent event) {
        var eventEntity = entityConverter.convert(event);
        eventEntity.setHeight(blockchain.getHeight());
        if (eventEntity.getHeight() != event.getHeight()) {
            var errMsg = String.format("Different height value: blockchain height=%d, event height=%d", eventEntity.getHeight(), event.getHeight());
            log.error(errMsg);
            throw new IllegalStateException(errMsg);
        }
        var logEntry = logEntryConverter.convert(event);
        logEntry.setHeight(blockchain.getHeight());

        log.debug("Save contract event at height {}, event={}", eventEntity.getHeight(), eventEntity.getLogInfo());
        log.debug("Check if exists, contract={} name={} signature={}", eventEntity.getContract(), eventEntity.getName(), toHex(eventEntity.getSignature()));
        var oldEvent = contractEventTable.get(contractEventTable.getDbKeyFactory().newKey(eventEntity));
        if (oldEvent == null || oldEvent.isNew()) {
            log.debug("Insert new event contract={} name={} signature={}", eventEntity.getContract(), eventEntity.getName(), toHex(eventEntity.getSignature()));
            //save the new contract event type
            contractEventTable.insert(eventEntity);
        }
        log.debug("Insert contract event log at height {}, log={}", eventEntity.getHeight(), logEntry.getLogInfo());
        contractEventLogTable.insert(logEntry);
    }

    @Override
    public void fireCdiEvent(AplContractEvent event) {
        cdiContractEvent.select(literal(SmcEventType.EMIT_EVENT)).fireAsync(event);
    }
}
