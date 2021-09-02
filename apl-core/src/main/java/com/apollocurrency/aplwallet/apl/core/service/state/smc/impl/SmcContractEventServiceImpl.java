/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventLogTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEventEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEventLogEntry;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.event.SmcContractEvent;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractEventServiceImpl implements SmcContractEventService {

    private final Blockchain blockchain;
    private final SmcContractEventTable contractEventTable;
    private final SmcContractEventLogTable contractEventLogTable;

    private final SmcConfig smcConfig;
    private final HashSumProvider hashSumProvider;

    @Inject
    public SmcContractEventServiceImpl(Blockchain blockchain, SmcContractEventTable contractEventTable, SmcContractEventLogTable contractEventLogTable, SmcConfig smcConfig) {
        this.blockchain = blockchain;
        this.contractEventTable = contractEventTable;
        this.contractEventLogTable = contractEventLogTable;
        this.smcConfig = smcConfig;
        this.hashSumProvider = smcConfig.createHashSumProvider();
    }

    @Override
    @Transactional
    public void saveEvent(SmcContractEvent event) {
        var eventType = SmcContractEventEntity.builder()
            .contract(event.getContract())
            .id(event.getId())
            .signature(event.getSignature())
            .name(event.getName())
            .idxCount(event.getIdxCount())
            .anonymous(event.isAnonymous())
            .transactionId(event.getTransactionId())
            .height(blockchain.getHeight())
            .build();

        var logEntry = SmcContractEventLogEntry.builder()
            .eventId(event.getId())
            .signature(event.getSignature())
            .transactionId(event.getTransactionId())
            .entry(event.getEntry())
            .height(blockchain.getHeight())
            .build();

        log.debug("Save the contract event at height {}, event={}, log={}", eventType.getHeight(), eventType, logEntry);

        var oldEvent = contractEventTable.get(contractEventTable.getDbKeyFactory().newKey(eventType));
        if (oldEvent == null || oldEvent.isNew()) {
            //save the new contract event type
            contractEventTable.insert(eventType);
        }
        contractEventLogTable.insert(logEntry);
    }
}
